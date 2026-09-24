//! Execução local dos componentes do Valora distribuído: PostgreSQL e backend (Spring Boot).
//!
//! Só é ativado quando os runtimes empacotados existem em `<recursos>/runtime/`:
//!
//! ```text
//! runtime/java/bin/java(.exe)            runtime Java 21 reduzido (jlink)
//! runtime/backend/valora-backend.jar     backend Spring Boot
//! runtime/postgresql/                    binários portáteis do PostgreSQL 17 (layout por plataforma)
//! ```
//!
//! O que muda entre Windows e Linux fica em plataforma.rs.
//!
//! Em desenvolvimento (`npm run tauri dev`) essa pasta não existe: nada é iniciado e o backend e o
//! PostgreSQL continuam sendo iniciados separadamente.
//!
//! Os dados ficam fora da pasta do programa (`<dados locais do usuário>/Valora`):
//! `pgdata/` (cluster do PostgreSQL), `config/` (senhas geradas na primeira execução) e `logs/`.
//! Tudo escuta somente em 127.0.0.1. Nada disso é exposto ao frontend: não há comandos Tauri.

use std::fs::{self, File, OpenOptions};
use std::io::{self, Write};
use std::net::{SocketAddr, TcpStream};
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::time::Duration;

use crate::plataforma::{self, comando, executavel, Vinculo};

/// Porta do PostgreSQL do Valora: fora da 5432 para não conflitar com outro PostgreSQL/Docker.
pub const PORTA_POSTGRESQL: u16 = 54329;
/// Porta do backend: a mesma usada pelo build desktop do frontend (vite.config.ts).
pub const PORTA_BACKEND: u16 = 8080;

const HOST: &str = "127.0.0.1";
const SUPERUSUARIO: &str = "postgres";
/// Usuário da aplicação: dono do banco, sem privilégios de superusuário.
const USUARIO_BANCO: &str = "valora";
const BANCO: &str = "valora";
/// Somente as origens do Tauri: no app distribuído não há Vite.
const ORIGENS_CORS: &str = "tauri://localhost,http://tauri.localhost";

/// Caminhos dos runtimes empacotados junto do programa.
pub struct Runtime {
  java: PathBuf,
  backend_jar: PathBuf,
  postgresql: PathBuf,
}

impl Runtime {
  /// Localiza os runtimes em `<recursos>/runtime`; `None` quando não estão todos presentes.
  pub fn localizar(recursos: &Path) -> Option<Runtime> {
    let base = plataforma::caminho_simples(recursos.join("runtime"));
    let runtime = Runtime {
      java: base.join("java").join("bin").join(executavel("java")),
      backend_jar: base.join("backend").join("valora-backend.jar"),
      postgresql: base.join("postgresql"),
    };
    let completo = runtime.java.is_file()
      && runtime.backend_jar.is_file()
      && plataforma::postgresql_bin(&runtime.postgresql).join(executavel("pg_ctl")).is_file();
    completo.then_some(runtime)
  }

  fn postgresql(&self, programa: &str) -> Command {
    let mut comando = comando(plataforma::postgresql_bin(&self.postgresql).join(executavel(programa)));
    plataforma::preparar_postgresql(&mut comando, &self.postgresql);
    comando
  }
}

/// Diretórios de dados do Valora, separados da pasta do programa.
pub struct Diretorios {
  base: PathBuf,
  pgdata: PathBuf,
  config: PathBuf,
  logs: PathBuf,
}

impl Diretorios {
  pub fn em(base: PathBuf) -> Diretorios {
    Diretorios {
      pgdata: base.join("pgdata"),
      config: base.join("config"),
      logs: base.join("logs"),
      base,
    }
  }

  fn criar(&self) -> io::Result<()> {
    for dir in [&self.base, &self.config, &self.logs] {
      fs::create_dir_all(dir)?;
    }
    Ok(())
  }

  pub fn logs(&self) -> &Path {
    &self.logs
  }

  /// Registra uma mensagem em `logs/valora-desktop.log` (o executável não tem console no Windows).
  pub fn registrar(&self, mensagem: &str) {
    let _ = fs::create_dir_all(&self.logs);
    if let Ok(mut arquivo) = abrir_log(&self.logs.join("valora-desktop.log")) {
      let _ = writeln!(arquivo, "{mensagem}");
    }
  }
}

/// Componentes em execução, encerrados por [`ComponentesLocais::encerrar`] ao fechar o app.
pub struct ComponentesLocais {
  runtime: Runtime,
  dirs: Diretorios,
  backend: Option<Child>,
  postgresql_iniciado: bool,
  /// Mantém o backend preso ao processo do app; precisa viver enquanto o app roda.
  _vinculo: Option<Vinculo>,
}

impl ComponentesLocais {
  /// Prepara os dados, inicia o PostgreSQL e dispara o backend (que termina de subir sozinho;
  /// o frontend aguarda a API responder). Em caso de erro, encerra o que já tiver iniciado.
  pub fn iniciar(runtime: Runtime, dirs: Diretorios) -> Result<ComponentesLocais, String> {
    let mut componentes = ComponentesLocais {
      runtime,
      dirs,
      backend: None,
      postgresql_iniciado: false,
      _vinculo: None,
    };
    match componentes.iniciar_etapas() {
      Ok(()) => Ok(componentes),
      Err(erro) => {
        componentes.encerrar();
        Err(erro)
      }
    }
  }

  fn iniciar_etapas(&mut self) -> Result<(), String> {
    self.dirs.criar().map_err(|e| format!("não foi possível criar os diretórios de dados: {e}"))?;
    let senha_superusuario = ler_ou_criar_segredo(&self.dirs.config.join("postgres.senha"))?;
    let senha_banco = ler_ou_criar_segredo(&self.dirs.config.join("banco.senha"))?;

    if !self.dirs.pgdata.join("PG_VERSION").is_file() {
      self.inicializar_cluster(&senha_superusuario)?;
    }
    self.iniciar_postgresql()?;
    self.preparar_banco(&senha_superusuario, &senha_banco)?;
    self.iniciar_backend(&senha_banco)
  }

  /// Primeira execução: cria o cluster com autenticação por senha (scram-sha-256).
  fn inicializar_cluster(&self, senha_superusuario: &str) -> Result<(), String> {
    if self.dirs.pgdata.exists() {
      return Err(format!(
        "{} existe, mas não contém um banco do PostgreSQL; verifique a pasta antes de continuar",
        self.dirs.pgdata.display()
      ));
    }
    // A senha vai por arquivo (não pela linha de comando) e o arquivo é removido em seguida.
    let arquivo_senha = self.dirs.config.join("initdb.senha");
    gravar_privado(&arquivo_senha, senha_superusuario)
      .map_err(|e| format!("não foi possível preparar a senha do banco: {e}"))?;
    let resultado = executar(
      self
        .runtime
        .postgresql("initdb")
        .arg("-D")
        .arg(&self.dirs.pgdata)
        .args(["-U", SUPERUSUARIO, "-A", "scram-sha-256", "-E", "UTF8"])
        // Mensagens do servidor em inglês (C); ordenação de texto pelo ICU, independente do SO.
        .args(["--locale=C", "--locale-provider=icu", "--icu-locale=und"])
        .arg(format!("--pwfile={}", arquivo_senha.display())),
      &self.dirs.logs.join("initdb.log"),
      "initdb",
    );
    let _ = fs::remove_file(&arquivo_senha);
    resultado?;

    // Somente loopback, porta própria e sem socket Unix (irrelevante no Windows).
    let mut conf = OpenOptions::new()
      .append(true)
      .open(self.dirs.pgdata.join("postgresql.conf"))
      .map_err(|e| format!("não foi possível configurar o PostgreSQL: {e}"))?;
    writeln!(
      conf,
      "\n# Valora: acesso somente local; JIT desligado (o pacote não inclui o LLVM)\nlisten_addresses = '{HOST}'\nport = {PORTA_POSTGRESQL}\nunix_socket_directories = ''\njit = off"
    )
    .map_err(|e| format!("não foi possível configurar o PostgreSQL: {e}"))
  }

  fn iniciar_postgresql(&mut self) -> Result<(), String> {
    // Um PostgreSQL deste mesmo diretório que ficou ativo (ex.: queda do app) é reaproveitado.
    let status = self
      .runtime
      .postgresql("pg_ctl")
      .arg("status")
      .arg("-D")
      .arg(&self.dirs.pgdata)
      .stdout(Stdio::null())
      .stderr(Stdio::null())
      .status()
      .map_err(|e| format!("não foi possível executar pg_ctl: {e}"))?;
    if status.success() {
      self.postgresql_iniciado = true;
      return Ok(());
    }
    if porta_em_uso(PORTA_POSTGRESQL) {
      return Err(format!(
        "a porta {PORTA_POSTGRESQL} já está em uso por outro programa; o banco do Valora não pode iniciar"
      ));
    }
    executar(
      self
        .runtime
        .postgresql("pg_ctl")
        .arg("start")
        .arg("-D")
        .arg(&self.dirs.pgdata)
        .arg("-l")
        .arg(self.dirs.logs.join("postgresql.log"))
        .args(["-w", "-t", "60"]),
      &self.dirs.logs.join("pg_ctl.log"),
      "pg_ctl start",
    )?;
    self.postgresql_iniciado = true;
    Ok(())
  }

  /// Cria (se preciso) o usuário e o banco da aplicação e sincroniza a senha com `config/`.
  /// Idempotente: roda a cada inicialização.
  fn preparar_banco(&self, senha_superusuario: &str, senha_banco: &str) -> Result<(), String> {
    // As senhas são hexadecimais (sem aspas) e vão pela entrada padrão, não pela linha de comando.
    let script = format!(
      "SELECT 'CREATE ROLE {USUARIO_BANCO} LOGIN' \
       WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '{USUARIO_BANCO}')\\gexec\n\
       ALTER ROLE {USUARIO_BANCO} WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '{senha_banco}';\n\
       SELECT 'CREATE DATABASE {BANCO} OWNER {USUARIO_BANCO}' \
       WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '{BANCO}')\\gexec\n"
    );
    let mut psql = self
      .runtime
      .postgresql("psql")
      .args(["-h", HOST, "-p", &PORTA_POSTGRESQL.to_string(), "-U", SUPERUSUARIO])
      .args(["-d", "postgres", "-X", "-q", "-v", "ON_ERROR_STOP=1", "-f", "-"])
      .env("PGPASSWORD", senha_superusuario)
      .stdin(Stdio::piped())
      .stdout(Stdio::null())
      .stderr(Stdio::from(
        abrir_log(&self.dirs.logs.join("psql.log")).map_err(|e| e.to_string())?,
      ))
      .spawn()
      .map_err(|e| format!("não foi possível executar psql: {e}"))?;
    if let Some(mut entrada) = psql.stdin.take() {
      entrada
        .write_all(script.as_bytes())
        .map_err(|e| format!("não foi possível preparar o banco: {e}"))?;
    }
    let status = psql.wait().map_err(|e| e.to_string())?;
    if status.success() {
      Ok(())
    } else {
      Err(format!("falha ao preparar o banco (ver {})", self.dirs.logs.join("psql.log").display()))
    }
  }

  fn iniciar_backend(&mut self, senha_banco: &str) -> Result<(), String> {
    if porta_em_uso(PORTA_BACKEND) {
      return Err(format!(
        "a porta {PORTA_BACKEND} já está em uso por outro programa; o backend do Valora não pode iniciar"
      ));
    }
    let log = abrir_log(&self.dirs.logs.join("backend.log")).map_err(|e| e.to_string())?;
    let log_erros = log.try_clone().map_err(|e| e.to_string())?;
    // Segredo do JWT novo a cada abertura: as sessões valem só enquanto o app está aberto.
    let segredo_jwt = segredo_hex(48)?;
    let mut java = comando(&self.runtime.java);
    Vinculo::preparar(&mut java);
    let backend = java
      .args(["-Xmx512m", "-jar"])
      .arg(&self.runtime.backend_jar)
      // Diretório de dados: o backend nunca lê um .env de desenvolvimento.
      .current_dir(&self.dirs.base)
      .env("POSTGRES_HOST", HOST)
      .env("POSTGRES_PORT", PORTA_POSTGRESQL.to_string())
      .env("POSTGRES_DB", BANCO)
      .env("POSTGRES_USER", USUARIO_BANCO)
      .env("POSTGRES_PASSWORD", senha_banco)
      .env("JWT_SECRET", segredo_jwt)
      .env("SERVER_ADDRESS", HOST)
      .env("SERVER_PORT", PORTA_BACKEND.to_string())
      .env("CORS_ORIGENS_PERMITIDAS", ORIGENS_CORS)
      .stdin(Stdio::null())
      .stdout(Stdio::from(log))
      .stderr(Stdio::from(log_erros))
      .spawn()
      .map_err(|e| format!("não foi possível iniciar o backend: {e}"))?;
    match Vinculo::vincular(&backend) {
      Ok(vinculo) => self._vinculo = Some(vinculo),
      Err(e) => self.dirs.registrar(&format!("Aviso: backend não vinculado ao app: {e}")),
    }
    self.backend = Some(backend);
    Ok(())
  }

  /// Encerra o backend e para o PostgreSQL (modo fast: conexões encerradas, dados preservados).
  pub fn encerrar(&mut self) {
    if let Some(mut backend) = self.backend.take() {
      let _ = backend.kill();
      let _ = backend.wait();
    }
    if self.postgresql_iniciado {
      self.postgresql_iniciado = false;
      let resultado = executar(
        self
          .runtime
          .postgresql("pg_ctl")
          .arg("stop")
          .arg("-D")
          .arg(&self.dirs.pgdata)
          .args(["-m", "fast", "-w", "-t", "60"]),
        &self.dirs.logs.join("pg_ctl.log"),
        "pg_ctl stop",
      );
      if let Err(erro) = resultado {
        self.dirs.registrar(&format!("Erro ao parar o PostgreSQL: {erro}"));
      }
    }
  }
}

/// Executa até o fim, com a saída no log informado.
fn executar(comando: &mut Command, log: &Path, descricao: &str) -> Result<(), String> {
  let saida = abrir_log(log).map_err(|e| e.to_string())?;
  let erros = saida.try_clone().map_err(|e| e.to_string())?;
  let status = comando
    .stdin(Stdio::null())
    .stdout(Stdio::from(saida))
    .stderr(Stdio::from(erros))
    .status()
    .map_err(|e| format!("não foi possível executar {descricao}: {e}"))?;
  if status.success() {
    Ok(())
  } else {
    Err(format!("{descricao} falhou ({status}); ver {}", log.display()))
  }
}

fn abrir_log(caminho: &Path) -> io::Result<File> {
  OpenOptions::new().create(true).append(true).open(caminho)
}

fn porta_em_uso(porta: u16) -> bool {
  let endereco = SocketAddr::from(([127, 0, 0, 1], porta));
  TcpStream::connect_timeout(&endereco, Duration::from_millis(300)).is_ok()
}

fn segredo_hex(bytes: usize) -> Result<String, String> {
  let mut buffer = vec![0u8; bytes];
  getrandom::fill(&mut buffer).map_err(|e| format!("não foi possível gerar um segredo: {e}"))?;
  Ok(buffer.iter().map(|b| format!("{b:02x}")).collect())
}

/// Lê o segredo guardado em `config/` ou gera um novo na primeira execução.
fn ler_ou_criar_segredo(caminho: &Path) -> Result<String, String> {
  if let Ok(conteudo) = fs::read_to_string(caminho) {
    let segredo = conteudo.trim();
    if segredo.len() >= 32 && segredo.chars().all(|c| c.is_ascii_hexdigit()) {
      return Ok(segredo.to_string());
    }
    return Err(format!("{} está inválido", caminho.display()));
  }
  let segredo = segredo_hex(32)?;
  gravar_privado(caminho, &segredo)
    .map_err(|e| format!("não foi possível gravar {}: {e}", caminho.display()))?;
  Ok(segredo)
}

/// Grava um arquivo legível somente pelo usuário (no Windows, a pasta de dados locais do usuário
/// já é restrita a ele).
fn gravar_privado(caminho: &Path, conteudo: &str) -> io::Result<()> {
  let mut opcoes = OpenOptions::new();
  opcoes.write(true).create(true).truncate(true);
  #[cfg(unix)]
  {
    use std::os::unix::fs::OpenOptionsExt;
    opcoes.mode(0o600);
  }
  opcoes.open(caminho)?.write_all(conteudo.as_bytes())
}
