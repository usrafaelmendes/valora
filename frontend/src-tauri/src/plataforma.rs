//! Tudo o que depende do sistema operacional no Valora desktop. Windows é a plataforma principal;
//! Linux é suportado. O restante do código (componentes_locais.rs) é comum às duas.
//!
//! | Assunto                        | Windows                            | Linux                                   |
//! |--------------------------------|------------------------------------|-----------------------------------------|
//! | Executáveis                    | `.exe`, sem janela de console      | sem extensão                            |
//! | Binários do PostgreSQL         | `postgresql/bin` (pacote EDB)      | `postgresql/lib/postgresql/17/bin` (PGDG) |
//! | Bibliotecas do PostgreSQL      | DLLs junto dos executáveis         | `postgresql/bibliotecas` (LD_LIBRARY_PATH) |
//! | Backend termina junto com o app| Job Object (KILL_ON_JOB_CLOSE)     | `PR_SET_PDEATHSIG`                      |
//! | Mensagem de erro nativa        | `MessageBoxW`                      | diálogo GTK                             |

use std::path::{Path, PathBuf};
use std::process::{Child, Command};

/// Nome do executável na plataforma (`java` → `java.exe` no Windows).
pub fn executavel(nome: &str) -> String {
  if cfg!(windows) {
    format!("{nome}.exe")
  } else {
    nome.to_string()
  }
}

/// Caminho sem o prefixo verbatim do Windows (`\\?\C:\...`), que o Tauri devolve para a pasta de
/// recursos. Programas como o `initdb` derivam o caminho dos outros executáveis do próprio caminho
/// e não conseguem executá-los nesse formato. Caminhos de rede (`\\?\UNC\`) ficam como estão.
pub fn caminho_simples(caminho: PathBuf) -> PathBuf {
  if cfg!(windows) {
    if let Some(texto) = caminho.to_str() {
      if let Some(resto) = texto.strip_prefix(r"\\?\") {
        if !resto.starts_with(r"UNC\") {
          return PathBuf::from(resto);
        }
      }
    }
  }
  caminho
}

/// Processo filho sem janela de console no Windows.
pub fn comando(programa: impl AsRef<std::ffi::OsStr>) -> Command {
  #[allow(unused_mut)]
  let mut comando = Command::new(programa);
  #[cfg(windows)]
  {
    use std::os::windows::process::CommandExt;
    const CREATE_NO_WINDOW: u32 = 0x0800_0000;
    comando.creation_flags(CREATE_NO_WINDOW);
  }
  comando
}

/// Pasta dos executáveis do PostgreSQL dentro de `runtime/postgresql`.
///
/// No Linux os binários PGDG mantêm a árvore do pacote (`lib/postgresql/17/bin`,
/// `share/postgresql/17`): o PostgreSQL localiza `share/` pelo caminho relativo ao executável.
pub fn postgresql_bin(raiz: &Path) -> PathBuf {
  if cfg!(windows) {
    raiz.join("bin")
  } else {
    raiz.join("lib").join("postgresql").join("17").join("bin")
  }
}

/// Ambiente dos programas do PostgreSQL: no Linux, as bibliotecas empacotadas (ICU, libxml2, ...).
pub fn preparar_postgresql(_comando: &mut Command, _raiz: &Path) {
  #[cfg(target_os = "linux")]
  {
    let bibliotecas = _raiz.join("bibliotecas");
    if bibliotecas.is_dir() {
      _comando.env("LD_LIBRARY_PATH", bibliotecas);
    }
  }
}

/// Mantém o backend preso ao processo do app: se o app cair ou for finalizado à força, o sistema
/// operacional encerra o backend (sem isso, um Java órfão ocuparia a porta na próxima abertura).
/// O PostgreSQL não é vinculado: um servidor órfão é reaproveitado na próxima abertura.
pub struct Vinculo {
  #[cfg(windows)]
  _job: windows::Job,
}

impl Vinculo {
  /// Linux: o filho recebe SIGTERM quando a thread que o criou (a principal do app) termina.
  pub fn preparar(_comando: &mut Command) {
    #[cfg(target_os = "linux")]
    {
      use std::os::unix::process::CommandExt;
      // SAFETY: prctl é async-signal-safe e roda no filho entre fork e exec.
      unsafe {
        _comando.pre_exec(|| {
          if libc::prctl(libc::PR_SET_PDEATHSIG, libc::SIGTERM) == -1 {
            return Err(std::io::Error::last_os_error());
          }
          Ok(())
        });
      }
    }
  }

  /// Windows: inclui o processo num Job Object com KILL_ON_JOB_CLOSE; o `Vinculo` precisa ficar
  /// vivo enquanto o app roda (quando o app termina, o sistema fecha o handle e encerra o job).
  pub fn vincular(_processo: &Child) -> std::io::Result<Vinculo> {
    Ok(Vinculo {
      #[cfg(windows)]
      _job: windows::Job::criar_com(_processo)?,
    })
  }
}

/// Mensagem de erro nativa, antes de o app encerrar (o executável não tem console no Windows).
pub fn mostrar_erro(titulo: &str, mensagem: &str) {
  #[cfg(windows)]
  windows::mostrar_erro(titulo, mensagem);
  #[cfg(target_os = "linux")]
  {
    use gtk::prelude::*;
    let dialogo = gtk::MessageDialog::new(
      None::<&gtk::Window>,
      gtk::DialogFlags::MODAL,
      gtk::MessageType::Error,
      gtk::ButtonsType::Ok,
      mensagem,
    );
    dialogo.set_title(titulo);
    dialogo.run();
    // SAFETY: o diálogo foi criado nesta função e não é mais usado depois de destruído.
    unsafe { dialogo.destroy() };
  }
  #[cfg(not(any(windows, target_os = "linux")))]
  eprintln!("{titulo}: {mensagem}");
}

#[cfg(windows)]
mod windows {
  use windows_sys::Win32::Foundation::{CloseHandle, HANDLE};
  use windows_sys::Win32::System::JobObjects::{
    AssignProcessToJobObject, CreateJobObjectW, JobObjectExtendedLimitInformation,
    SetInformationJobObject, JOBOBJECT_EXTENDED_LIMIT_INFORMATION,
    JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE,
  };

  pub struct Job(HANDLE);

  // SAFETY: o handle do job pode ser usado e fechado a partir de qualquer thread.
  unsafe impl Send for Job {}

  impl Job {
    pub fn criar_com(processo: &std::process::Child) -> std::io::Result<Job> {
      use std::os::windows::io::AsRawHandle;
      // SAFETY: chamadas Win32 com ponteiros válidos; o handle é fechado em Drop.
      unsafe {
        let job = Job(CreateJobObjectW(std::ptr::null(), std::ptr::null()));
        if job.0.is_null() {
          return Err(std::io::Error::last_os_error());
        }
        let mut limites: JOBOBJECT_EXTENDED_LIMIT_INFORMATION = std::mem::zeroed();
        limites.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        let ok = SetInformationJobObject(
          job.0,
          JobObjectExtendedLimitInformation,
          &limites as *const _ as *const core::ffi::c_void,
          std::mem::size_of::<JOBOBJECT_EXTENDED_LIMIT_INFORMATION>() as u32,
        );
        if ok == 0 || AssignProcessToJobObject(job.0, processo.as_raw_handle() as HANDLE) == 0 {
          return Err(std::io::Error::last_os_error());
        }
        Ok(job)
      }
    }
  }

  impl Drop for Job {
    fn drop(&mut self) {
      // SAFETY: handle criado por CreateJobObjectW e fechado uma única vez.
      unsafe { CloseHandle(self.0) };
    }
  }

  pub fn mostrar_erro(titulo: &str, mensagem: &str) {
    use windows_sys::Win32::UI::WindowsAndMessaging::{MessageBoxW, MB_ICONERROR, MB_OK};
    let largo = |texto: &str| texto.encode_utf16().chain(Some(0)).collect::<Vec<u16>>();
    let (titulo, mensagem) = (largo(titulo), largo(mensagem));
    // SAFETY: textos UTF-16 terminados em zero, válidos durante a chamada.
    unsafe { MessageBoxW(std::ptr::null_mut(), mensagem.as_ptr(), titulo.as_ptr(), MB_OK | MB_ICONERROR) };
  }
}
