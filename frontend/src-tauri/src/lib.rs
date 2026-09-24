// Shell desktop do Valora: abre a janela com o React (Vite em desenvolvimento, `dist/` no build).
// Não há comandos nem plugins: regras e cálculos continuam no backend.
//
// No app distribuído (runtimes empacotados em `runtime/`), também inicia e encerra o PostgreSQL
// e o backend locais (ver componentes_locais.rs). Em desenvolvimento, nada é iniciado.
mod componentes_locais;
mod plataforma;

use std::sync::Mutex;

use componentes_locais::{ComponentesLocais, Diretorios, Runtime};
use tauri::{Manager, RunEvent};

/// Nome da pasta de dados em `%LOCALAPPDATA%` (Windows) ou `~/.local/share` (Linux).
const PASTA_DADOS: &str = "Valora";

struct Estado(Mutex<Option<ComponentesLocais>>);

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .manage(Estado(Mutex::new(None)))
    .setup(|app| {
      let Some(runtime) = Runtime::localizar(&app.path().resource_dir()?) else {
        return Ok(());
      };
      let dirs = Diretorios::em(pasta_dados(app)?);
      dirs.registrar("Iniciando o PostgreSQL e o backend locais");
      let logs = dirs.logs().to_path_buf();
      match ComponentesLocais::iniciar(runtime, dirs) {
        Ok(componentes) => {
          *app.state::<Estado>().0.lock().unwrap() = Some(componentes);
        }
        // Sem o banco ou o backend o Valora não funciona: avisa e encerra (o que já tinha
        // iniciado foi encerrado por `iniciar`).
        Err(erro) => {
          Diretorios::em(pasta_dados(app)?).registrar(&format!("Erro: {erro}"));
          plataforma::mostrar_erro(
            "Valora",
            &format!(
              "O Valora não conseguiu iniciar.\n\n{erro}\n\nDetalhes nos registros em:\n{}",
              logs.display()
            ),
          );
          std::process::exit(1);
        }
      }
      Ok(())
    })
    .build(tauri::generate_context!())
    .expect("erro ao iniciar a aplicação Tauri")
    .run(|app, evento| {
      if let RunEvent::Exit = evento {
        if let Some(mut componentes) = app.state::<Estado>().0.lock().unwrap().take() {
          componentes.encerrar();
        }
      }
    });
}

fn pasta_dados(app: &tauri::App) -> tauri::Result<std::path::PathBuf> {
  // Somente em builds de depuração: permite testar com uma pasta de dados temporária.
  #[cfg(debug_assertions)]
  if let Some(pasta) = std::env::var_os("VALORA_DATA_DIR") {
    return Ok(pasta.into());
  }
  Ok(app.path().local_data_dir()?.join(PASTA_DADOS))
}
