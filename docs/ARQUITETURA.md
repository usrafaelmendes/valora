# Arquitetura do Sistema — Valora

## 1. Objetivo

Este documento define a arquitetura técnica do Valora.

O objetivo é estabelecer a organização dos principais componentes do sistema, suas responsabilidades e a forma como eles se comunicam.

A arquitetura deverá ser simples, modular e adequada ao escopo do MVP.

---

## 2. Visão Geral

O sistema será dividido principalmente em três partes:

- Frontend;
- Backend;
- Banco de dados.

A comunicação entre frontend e backend será realizada por meio de uma API REST.

Visão simplificada:

Frontend Web
↓
API REST
↓
Backend Spring Boot
↓
PostgreSQL

---

## 3. Frontend

O frontend será responsável pela interface utilizada pelos usuários.

Suas principais responsabilidades serão:

- apresentar as telas do sistema;
- realizar login;
- permitir consultas e cotações;
- apresentar fornecedores e produtos;
- apresentar os resultados das comparações;
- permitir o upload de arquivos quando autorizado;
- apresentar mensagens de erro e sucesso;
- permitir o download da tabela de cotação.

O frontend não deverá implementar as regras tributárias principais.

Os cálculos e regras de negócio deverão permanecer no backend.

---

## 4. Backend

O backend será desenvolvido em:

- Java;
- Spring Boot.

O backend será responsável por:

- autenticação e autorização;
- gerenciamento de usuários;
- gerenciamento de fornecedores;
- gerenciamento de produtos;
- recebimento de arquivos XML;
- processamento das NF-e;
- aplicação das regras de negócio;
- cálculo do custo efetivo;
- comparação entre fornecedores;
- comunicação com o banco de dados;
- disponibilização da API REST.

---

## 5. API REST

A API REST será o principal meio de comunicação entre frontend e backend.

De forma geral:

Frontend → requisição HTTP → API → processamento → resposta HTTP → Frontend

Os endpoints deverão ser organizados de acordo com os principais recursos do sistema.

Exemplos conceituais:

- `/auth`
- `/usuarios`
- `/fornecedores`
- `/produtos`
- `/nfe`
- `/cotacoes`
- `/comparacoes`

Os endpoints definitivos serão definidos durante a implementação.

---

## 6. Organização do Backend

O backend deverá possuir uma estrutura modular.

Uma organização inicial sugerida é:

backend/
└── src/
    └── main/
        └── java/
            └── .../
                ├── config/
                ├── controller/
                ├── dto/
                ├── entity/
                ├── repository/
                ├── service/
                ├── exception/
                └── calculation/

### config

Responsável pelas configurações da aplicação, segurança e demais configurações necessárias.

### controller

Responsável por receber as requisições HTTP e retornar as respostas da API.

### dto

Responsável pelos objetos utilizados na comunicação da API.

### entity

Responsável pelas entidades persistidas no banco de dados.

### repository

Responsável pelo acesso aos dados persistidos.

### service

Responsável pelas principais regras de negócio e orquestração das operações.

### exception

Responsável pelo tratamento padronizado de erros e exceções.

### calculation

Responsável pela lógica de cálculo e comparação do custo efetivo.

A estrutura poderá ser ajustada durante o desenvolvimento caso seja identificada uma organização mais adequada.

---

## 7. Banco de Dados

O sistema utilizará PostgreSQL.

O banco será executado durante o desenvolvimento por meio do Docker.

As informações deverão ser persistidas de forma estruturada.

Entre os principais dados esperados estão:

- usuários;
- fornecedores;
- produtos;
- NF-e;
- itens de NF-e;
- informações tributárias;
- regras tributárias configuráveis;
- dados do fornecedor relevantes ao cálculo (UF de emissão, tipo fabricante/atacadista e prazo de pagamento base);
- cotações;
- opções de fornecedores;
- resultados de comparação.

O modelo definitivo do banco será detalhado durante a implementação.

---

## 8. Processamento de NF-e

O fluxo inicial de NF-e será manual.

O administrador realizará o upload de um arquivo XML pelo sistema.

Fluxo:

1. Administrador seleciona o XML.
2. Frontend envia o arquivo para o backend.
3. Backend valida o arquivo.
4. Backend processa o XML.
5. Backend extrai as informações relevantes.
6. Backend valida os dados extraídos.
7. Backend armazena as informações no PostgreSQL.
8. Sistema informa o resultado do processamento.

O MVP não dependerá de busca automática de documentos fiscais na SEFAZ.

Não será necessário utilizar certificado digital para essa funcionalidade no MVP.

Os testes usam somente XMLs sintéticos (`backend/src/test/resources/nfe/`). Arquivos reais de NF-e, se usados localmente, ficam em `test-data/nfe/`, que é ignorada pelo Git: dados reais não são versionados.

O estado de origem da mercadoria corresponde ao estado de emissão da NF-e, que pode ser obtido pelo CNPJ do fornecedor.

---

## 9. Módulo de Cálculo

O cálculo do custo efetivo será realizado no backend.

O fluxo conceitual será:

1. Receber os dados da cotação.
2. Identificar o produto.
3. Identificar o fornecedor.
4. Identificar as informações tributárias aplicáveis.
5. Aplicar as regras tributárias configuradas.
6. Calcular os créditos aplicáveis.
7. Calcular o custo efetivo.
8. Retornar o resultado.

A fórmula definitiva deverá seguir `REGRAS_TRIBUTARIAS.md`.

Uma instalação nova começa **sem regras tributárias pré-configuradas** e com os parâmetros de cálculo não definidos (exceto os técnicos): o ADMIN configura as regras e os parâmetros que se aplicam à sua operação, e até lá os cálculos são informados como incompletos.

Cada crédito deverá ser calculado individualmente sobre o valor original da operação (sem aplicação em cascata), e o total de créditos deverá ser subtraído desse valor. As regras deverão ser configuráveis e o resultado deverá manter a rastreabilidade de cada crédito considerado.

O frontend não deverá duplicar essa lógica.

---

## 10. Módulo de Comparação

O módulo de comparação será responsável por analisar as opções disponíveis para determinado produto.

Fluxo conceitual:

1. Receber as opções de fornecedores.
2. Calcular o custo efetivo de cada opção.
3. Comparar os resultados.
4. Ordenar as opções.
5. Retornar a lista completa ao frontend.

O sistema deverá manter as alternativas disponíveis e não somente a primeira colocada.

---

## 11. Autenticação e Autorização

O sistema deverá possuir autenticação de usuários.

Deverão existir pelo menos dois perfis:

- ADMIN;
- USER.

O backend deverá controlar o acesso às funcionalidades de acordo com o perfil autenticado.

Operações administrativas deverão exigir permissão de administrador.

### 11.1 Configuração inicial (primeiro ADMIN)

Uma instalação nova começa sem usuários e sem credenciais administrativas no ambiente (RF01.1):

- `GET /auth/configuracao-inicial` (público) informa somente `{ "configurado": true | false }`, isto é, se já existe algum usuário;
- `POST /auth/configuracao-inicial` (público) recebe nome, e-mail, senha e confirmação da senha e cria o primeiro usuário, sempre com perfil ADMIN e senha em BCrypt; se já existir qualquer usuário, responde `409`;
- a verificação e a criação ocorrem na mesma transação, após bloquear a tabela `usuario` para inserções (`LOCK TABLE ... IN EXCLUSIVE MODE`): requisições simultâneas são serializadas e somente uma cria o ADMIN;
- no frontend, a tela de login consulta o status e, com `configurado: false`, leva à tela `/configuracao-inicial`; após a criação, o usuário volta ao login para entrar. Com o sistema configurado, a tela de configuração inicial leva ao login (ou ao início, com sessão ativa).

---

## 12. Fluxo Principal do Sistema

O fluxo principal esperado para uma comparação será:

1. Usuário realiza login.
2. Usuário acessa a área de cotação.
3. Usuário informa ou seleciona o produto.
4. Sistema identifica as opções de fornecedores disponíveis.
5. Backend processa os dados necessários.
6. Backend aplica as regras tributárias.
7. Backend calcula o custo efetivo.
8. Backend compara as opções.
9. Sistema apresenta as opções ordenadas.
10. Usuário visualiza a melhor opção e as alternativas.
11. Usuário pode baixar a tabela da cotação.

---

## 13. Separação de Responsabilidades

As responsabilidades deverão permanecer separadas.

### Frontend

Responsável principalmente por:

- interface;
- interação com o usuário;
- envio de requisições;
- apresentação dos resultados.

### Backend

Responsável principalmente por:

- regras de negócio;
- cálculos;
- autenticação;
- processamento de NF-e;
- validações;
- acesso aos dados.

### Banco de dados

Responsável por:

- persistência;
- consulta;
- atualização;
- relacionamento dos dados.

---

## 14. Configuração e Ambiente

Informações específicas do ambiente não deverão ser armazenadas diretamente no código-fonte.

Variáveis de ambiente deverão ser utilizadas para informações como:

- configuração do banco;
- usuário do banco;
- senha do banco;
- configurações específicas do ambiente.

Arquivos contendo informações sensíveis não deverão ser versionados no Git.

---

## 15. Docker

O PostgreSQL será executado por meio do Docker durante o desenvolvimento.

O arquivo `compose.yaml` será responsável pela configuração do serviço do banco.

A aplicação poderá utilizar posteriormente containers adicionais caso isso seja necessário.

Não deverão ser adicionados containers ou serviços desnecessários ao MVP.

---

## 16. Estrutura Geral do Projeto

A estrutura inicial do projeto será:

valora/
├── backend/
├── frontend/
├── docs/
│   ├── REQUISITOS.md
│   ├── REGRAS_TRIBUTARIAS.md
│   ├── ARQUITETURA.md
│   └── CASOS_DE_TESTE.md
├── test-data/
│   └── nfe/
├── .env
├── .env.example
├── .gitignore
├── compose.yaml
└── README.md

---

## 17. Princípios Arquiteturais

O desenvolvimento deverá seguir os seguintes princípios:

- manter o sistema simples dentro do escopo do MVP;
- evitar complexidade desnecessária;
- manter responsabilidades bem separadas;
- evitar duplicação de regras de negócio;
- manter as regras tributárias centralizadas no backend;
- manter configurações sensíveis fora do código;
- priorizar código legível e organizado;
- permitir evolução futura do sistema;
- testar as funcionalidades antes de avançar para etapas dependentes.

---

## 18. Decisões que Ainda Podem ser Refinadas

Algumas decisões técnicas poderão ser definidas durante a implementação, desde que não contrariem os requisitos do projeto.

Entre elas:

- tecnologia específica do frontend (definida, ver §18.1);
- estratégia definitiva de autenticação;
- biblioteca utilizada para leitura do XML;
- ferramenta de migrations do banco;
- formato definitivo do arquivo de download;
- estrutura final das entidades;
- endpoints definitivos da API;
- forma de obtenção do estado de origem (cadastro do fornecedor via CNPJ ou emitente do XML);
- forma de carga inicial de dados de produtos e fornecedores a partir de outros sistemas;
- regra de arredondamento dos créditos.

Essas decisões deverão ser documentadas quando forem tomadas.

### 18.1 Tecnologia do frontend (decidida)

Aplicação SPA em `frontend/`:

- React 19 + TypeScript (modo `strict`) + Vite;
- React Router para rotas; Mantine como biblioteca de componentes;
- TanStack Query para chamadas à API; `fetch` nativo em um cliente HTTP central (`src/api/httpClient.ts`);
- Vitest + React Testing Library para testes; ESLint (`typescript-eslint`, `react-hooks`, `jsx-a11y`) e Prettier para qualidade.
- Playwright (somente desenvolvimento) para os testes E2E do sistema completo (`frontend/e2e/`, `npm run e2e`): PostgreSQL do `compose.yaml` com banco temporário exclusivo e vazio, backend (jar) e frontend (Vite), com dados fictícios e segredo JWT/senha do ADMIN gerados a cada execução; o primeiro teste cria o ADMIN pela tela de configuração inicial.

Autenticação no frontend:

- em instalação nova (sem usuários), a tela de login leva à configuração inicial (§11.1);
- o JWT emitido por `POST /auth/login` fica em `sessionStorage` e é enviado como `Authorization: Bearer`;
- após reload, a sessão é confirmada em `GET /auth/me`; o perfil (ADMIN/USER) vem sempre do backend;
- `401` em chamada autenticada encerra a sessão e leva ao login; não há refresh token;
- a proteção de rotas por perfil no frontend é só de interface: a autorização continua no backend.

Comunicação: a URL base da API fica em um único ponto (`src/config/env.ts`, `API_BASE_URL`), usada pelo cliente HTTP central; endpoints, JWT e comportamento são os mesmos na web e no desktop. O endereço do backend local (`http://localhost:8080`) é definido somente em `vite.config.ts`.

- **Web** (`npm run dev`): o frontend chama `/api/*` e o proxy do Vite repassa ao backend sem o prefixo, na mesma origem para o navegador. Em produção web, o mesmo papel deverá ser feito por um proxy reverso (ainda não definido).

```text
React/Vite (navegador)
   ↓
proxy /api
   ↓
Spring Boot localhost:8080
   ↓
PostgreSQL localhost
```

- **Desktop** (Tauri, modo `desktop` do Vite: `npm run dev:desktop` / `npm run build:desktop`, acionados pelo `npm run tauri dev` / `npm run tauri build`): o frontend chama o backend local diretamente em `http://localhost:8080`, sem depender do proxy do Vite, que não existe no app empacotado.

```text
Tauri
   ↓
React
   ↓
Spring Boot localhost:8080
   ↓
PostgreSQL localhost
```

Como no desktop a origem da página (`tauri://localhost` no Linux/macOS, `http://tauri.localhost` no Windows e `http://localhost:5173` no `tauri dev`) difere da do backend, o backend habilita CORS somente para as origens de `app.cors.origens-permitidas` (variável `CORS_ORIGENS_PERMITIDAS`; padrão: essas três). Não há curinga nem cookies (`allowCredentials=false`): o token continua no header `Authorization`. São permitidos `GET/POST/PUT/DELETE`, os headers `Authorization`, `Content-Type` e `Accept`, e o header `Content-Disposition` é exposto (nome do CSV baixado). A origem do Vite também é necessária na web, porque o proxy repassa o header `Origin` do navegador.

O backend escuta por padrão somente no loopback (`server.address`, variável `SERVER_ADDRESS`, padrão `127.0.0.1`): não aceita conexões de outras máquinas da rede.

Shell desktop (Tauri 2, em `frontend/src-tauri/`): a mesma SPA abre em uma janela desktop, o **Valora** desktop (ver §18.2).

- o Tauri não expõe comandos, plugins nem permissões de APIs ao frontend (`capabilities/default.json` vazio); regras e cálculos continuam no backend;
- em desenvolvimento (`npm run tauri dev`), a janela carrega o servidor do Vite (`http://localhost:5173`) no modo `desktop`; no build (`npm run tauri build`), carrega o `dist/` gerado no modo `desktop`. Nos dois casos o frontend chama o backend local diretamente (acima), sem o proxy `/api`;
- `npm run tauri build` gera somente o executável da aplicação (`src-tauri/target/release/`), sem instalador (`bundle.active: false`).

O frontend não contém regra tributária nem cálculo de custo: apenas apresenta os dados da API.

### 18.2 Valora desktop: execução local e empacotamento

O desktop é uma aplicação local e offline que funciona sem WSL, Docker, Node.js, npm, Maven, Git, Java ou PostgreSQL instalados. A web continua disponível; o desktop é um modo adicional.

- **Plataforma principal:** Windows (x64), distribuída por instalador `.exe`.
- **Plataforma suportada:** Linux (x64), pacote `.deb`.
- macOS não é alvo.

**Desenvolvimento** (inalterado; backend e PostgreSQL iniciados separadamente; sem a pasta `runtime/` nada é iniciado pelo Tauri):

```text
Tauri (npm run tauri dev)
  ↓
React (Vite, modo desktop)
  ↓
Spring Boot local (mvn spring-boot:run, 127.0.0.1:8080)
  ↓
PostgreSQL Docker/local (compose.yaml, 127.0.0.1:5432)
```

**Windows** (instalador NSIS):

```text
Tauri (Valora.exe)
  ↓
React empacotado (dist/, modo desktop)
  ↓
Spring Boot empacotado (runtime Java 21 jlink, 127.0.0.1:8080)
  ↓
PostgreSQL portátil (EDB 17 para Windows, 127.0.0.1:54329)
```

**Linux** (pacote `.deb`):

```text
Tauri (valora)
  ↓
React empacotado (dist/, modo desktop)
  ↓
Spring Boot empacotado (runtime Java 21 jlink, 127.0.0.1:8080)
  ↓
PostgreSQL local portátil (PGDG 17 com bibliotecas empacotadas, 127.0.0.1:54329)
```

#### Inicialização (`src-tauri/src/componentes_locais.rs`)

O próprio Tauri (código Rust, `std::process`) inicia e encerra os componentes; não há plugin `shell`, comando Tauri nem permissão para o frontend (`capabilities/default.json` continua vazio). O mecanismo só é ativado quando os runtimes empacotados existem em `<recursos>/runtime/`; sem eles (desenvolvimento), nada é iniciado.

Primeira execução em uma máquina limpa:

1. o Tauri localiza `runtime/java`, `runtime/backend/valora-backend.jar` e `runtime/postgresql`;
2. cria as pastas de dados (`pgdata/`, `config/`, `logs/`);
3. gera as senhas do banco (superusuário e aplicação) em `config/`;
4. `initdb` cria o cluster;
5. `pg_ctl start` inicia o PostgreSQL;
6. `psql` cria, de forma idempotente, o usuário `valora` e o banco `valora`, e sincroniza a senha;
7. inicia o backend (`java -jar`) com a configuração por variáveis de ambiente e abre a janela;
8. o React consulta `GET /auth/configuracao-inicial` e, enquanto o backend não responde, repete a consulta a cada segundo (até 2 minutos), mostrando "Aguardando o servidor do Valora";
9. o Flyway cria o schema na subida do backend;
10. com o banco vazio, o frontend apresenta a configuração inicial (§11.1);
11. o primeiro ADMIN é criado pelo usuário. Não há usuário nem senha padrão.

Nas execuções seguintes, os passos 3, 4 e 10 não se repetem: o banco e as senhas são reaproveitados. Ao fechar a janela, o Tauri encerra o backend e para o PostgreSQL (`pg_ctl stop -m fast`).

O backend fica vinculado ao processo do app: se o app cair ou for finalizado à força, o sistema operacional encerra o Java, evitando um backend órfão ocupando a porta. O PostgreSQL não é vinculado: se ficar ativo, é reaproveitado na próxima abertura (`pg_ctl status`).

Configuração passada ao backend (variáveis de ambiente, nunca pela linha de comando nem por `.env`): `POSTGRES_HOST=127.0.0.1`, `POSTGRES_PORT=54329`, `POSTGRES_DB`/`POSTGRES_USER=valora`, `POSTGRES_PASSWORD` (gerada), `JWT_SECRET` (gerado a cada abertura: as sessões valem enquanto o app está aberto), `SERVER_ADDRESS=127.0.0.1`, `SERVER_PORT=8080` e `CORS_ORIGENS_PERMITIDAS` somente com as origens do Tauri. O diretório de trabalho do backend é a pasta de dados, para que nenhum `.env` de desenvolvimento seja lido.

**Falha ao iniciar** (ex.: porta 8080 ou 54329 ocupada por outro programa, `initdb` com erro): o erro é registrado em `logs/valora-desktop.log`, o que já tinha iniciado é encerrado e o Valora mostra uma mensagem nativa ("O Valora não conseguiu iniciar", com o motivo e a pasta de logs) e fecha. A porta do backend continua fixa (8080) nesta etapa; porta dinâmica não foi adotada.

#### Específico de cada sistema operacional (`src-tauri/src/plataforma.rs`)

| Assunto | Windows | Linux |
|---|---|---|
| Executável | `Valora.exe`, sem janela de console (`CREATE_NO_WINDOW`) | `valora` |
| Caminho dos recursos | prefixo verbatim `\\?\` removido (o `initdb` não executa o `postgres.exe` nesse formato) | sem ajuste |
| Binários do PostgreSQL | `runtime/postgresql/bin` (DLLs junto) | `runtime/postgresql/lib/postgresql/17/bin` (árvore do pacote, para o PostgreSQL localizar `share/`) |
| Bibliotecas do PostgreSQL | junto dos executáveis | `runtime/postgresql/bibliotecas`, via `LD_LIBRARY_PATH` só nos processos do PostgreSQL |
| Backend termina junto com o app | Job Object com `KILL_ON_JOB_CLOSE` | `PR_SET_PDEATHSIG` (SIGTERM) |
| Mensagem de erro nativa | `MessageBoxW` | diálogo GTK |
| Pasta de dados | `%LOCALAPPDATA%\Valora` | `~/.local/share/Valora` (`$XDG_DATA_HOME/Valora`) |
| Permissão das senhas | a pasta do usuário já é restrita a ele | arquivos com permissão `600` |

#### Pastas: programa × dados

| Responsabilidade | Windows | Linux (`.deb`) | Observação |
|---|---|---|---|
| Programa | `%LOCALAPPDATA%\Programs\Valora\` | `/usr/bin/valora` e `/usr/lib/Valora/runtime/` | substituído em atualizações |
| Dados | `%LOCALAPPDATA%\Valora\` | `~/.local/share/Valora/` | preservados em atualizações e na desinstalação |
| Banco | `...\Valora\pgdata\` | `.../Valora/pgdata/` | cluster do PostgreSQL |
| Configuração | `...\Valora\config\` | `.../Valora/config/` | `postgres.senha` (superusuário) e `banco.senha` (aplicação) |
| Logs | `...\Valora\logs\` | `.../Valora/logs/` | `valora-desktop.log`, `backend.log`, `postgresql.log`, `initdb.log`, `pg_ctl.log`, `psql.log` |

O WebView2 guarda a sessão do navegador embutido em `%LOCALAPPDATA%\com.comparaprecos.desktop` (pasta definida pelo identificador do Tauri), separada dos dados do Valora.

#### PostgreSQL por plataforma

Mesma versão (17) do `compose.yaml`, como binários portáteis controlados pelo Tauri: sem serviço do sistema, sem privilégio de administrador, sem conflito com outro PostgreSQL e sem Docker.

- **Windows:** pacote oficial "binaries only" da EDB (`postgresql-17.11-1-windows-x64-binaries.zip`), reduzido a `bin/`, `lib/` e `share/` mais as licenças (sem pgAdmin, StackBuilder, documentação e cabeçalhos), cerca de 120 MB;
- **Linux:** binários PGDG da imagem oficial `postgres:17.11-bookworm` (Debian 12, glibc 2.36), com as bibliotecas de que dependem (ICU, libxml2, OpenSSL, Kerberos, LDAP...), exceto as da glibc. Sem o LLVM: o JIT fica desligado (`jit = off`). Cerca de 105 MB. Exige uma distribuição com glibc ≥ 2.36.

Em ambos:

- **inicialização do banco**: `initdb` com `-E UTF8`, `--locale=C` e ordenação ICU (`--icu-locale=und`, ordem natural com acentos, independente do sistema), autenticação `scram-sha-256`;
- **usuários**: superusuário `postgres` (somente para manutenção pelo Tauri) e `valora`, dono do banco `valora`, **sem** privilégios de superusuário;
- **senhas**: aleatórias (32 bytes, gerador do sistema operacional), por instalação, em `config/`; entregues ao `initdb` por arquivo temporário e ao `psql` pela entrada padrão, nunca pela linha de comando. Nenhuma credencial no código, e nada do `.env` de desenvolvimento é reutilizado;
- **rede**: `listen_addresses = '127.0.0.1'`, porta `54329`, sem socket Unix;
- **migrações**: Flyway na subida do backend;
- **inicialização/parada**: `pg_ctl` (que também reduz privilégios caso o app rode como administrador no Windows).

#### Java por plataforma

Runtime **Java 21 reduzido com `jlink`** a partir dos jmods do **JDK Temurin 21.0.12.1** da plataforma de destino (o `jlink` do JDK local gera o runtime de outra plataforma): cerca de 55–65 MB, executado com `-Xmx512m`. Módulos levantados com `jdeps` sobre o jar do backend, mais `jdk.localedata` (pt-BR), `jdk.charsets`, `jdk.zipfs` e `java.naming`. O Maven continua sendo usado normalmente no desenvolvimento.

#### Empacotamento

Runtimes separados por plataforma em `src-tauri/runtime/` (ignorada pelo Git), gerados por `src-tauri/scripts/preparar-runtime.sh`, com versões e SHA-256 fixados no script e downloads em cache (`~/.cache/valora-runtime`):

```text
src-tauri/runtime/
├── windows-x86_64/   java/ (jlink Windows)  backend/valora-backend.jar  postgresql/ (EDB)
└── linux-x86_64/     java/ (jlink Linux)    backend/valora-backend.jar  postgresql/ (PGDG + bibliotecas)
```

A configuração base (`tauri.conf.json`) não inclui recursos nem instalador (`bundle.active: false`), para que `npm run tauri dev` e `npm run tauri build` continuem iguais. O empacotamento usa uma configuração adicional por plataforma, que mapeia `runtime/<plataforma>` para `runtime/` dentro do pacote e define o nome do executável:

| Comando (em `frontend/`) | O que faz |
|---|---|
| `npm run runtime:windows` / `runtime:linux` | monta `src-tauri/runtime/<plataforma>/` |
| `npm run empacotar:windows` | `Valora_<versão>_x64-setup.exe` (NSIS), compilado a partir do Linux/WSL com `cargo-xwin` (`tauri.empacotamento.windows.json`) |
| `npm run empacotar:linux` | `Valora_<versão>_amd64.deb` (`tauri.empacotamento.linux.json`) |

Máquina de build (não o usuário final): JDK 21, Maven, Node.js, Rust, `python3`, `curl`, Docker (só para extrair o PostgreSQL Linux), e para o Windows `cargo-xwin` e os pacotes `nsis lld llvm clang`. Na primeira execução, o `cargo-xwin` baixa o CRT e o SDK do Windows (licença da Microsoft).

#### Instalador Windows

- **Tipo:** NSIS. O MSI (WiX) só é gerado no Windows e, na prática, instala por máquina.
- **Instalação por usuário** (`installMode: currentUser`): não exige privilégio de administrador. Idioma: português do Brasil.
- **Diretório do programa:** `%LOCALAPPDATA%\Programs\Valora`. O padrão do Tauri seria `%LOCALAPPDATA%\Valora`, a mesma pasta dos dados; por isso o Valora usa um template NSIS próprio (`src-tauri/windows/installer.nsi`), cópia do template da CLI do Tauri 2.11.5 com essa única linha alterada. Ao atualizar a CLI do Tauri, o template deve ser recopiado e a alteração reaplicada.
- **Atualização:** instalar uma versão nova por cima substitui só o programa; os dados são preservados.
- **Desinstalação:** remove somente os arquivos instalados, e `%LOCALAPPDATA%\Valora` é preservada. A opção "apagar dados do aplicativo" do desinstalador remove só a pasta do WebView2 (`%LOCALAPPDATA%\com.comparaprecos.desktop`). Remover o banco continua sendo uma ação manual.
- **Assinatura digital e atualização automática:** não fazem parte desta etapa.

**WebView2:** o Windows 11 já traz o runtime do WebView2, e o Windows 10 atualizado costuma recebê-lo pelo Windows Update. O instalador verifica a presença dele e, se faltar, baixa e executa o instalador de bootstrap da Microsoft (`webviewInstallMode: downloadBootstrapper`, cerca de 2 MB, silencioso). Nesse caso é necessária internet durante a instalação. Um instalador totalmente offline exigiria embutir o instalador completo do WebView2 (cerca de 130 MB) e não foi adotado.

#### Linux

- `.deb` com dependências `libwebkit2gtk-4.1-0` e `libgtk-3-0`; programa em `/usr/lib/Valora`, dados em `~/.local/share/Valora`.
- **AppImage: não gerado.** O `linuxdeploy`, usado pelo Tauri para montar o AppImage, tenta resolver as dependências de todos os binários do pacote, inclusive do Java e do PostgreSQL empacotados, e falha (`libjvm.so`). Caminhos possíveis, a avaliar se o AppImage for necessário: montar a imagem com o `appimagetool` sobre o AppDir, ou manter os runtimes fora da varredura do `linuxdeploy`.
- Os binários do app e do runtime seguem a glibc da máquina de build; para ampliar a compatibilidade, gerar o pacote numa distribuição mais antiga (ex.: Debian 12).

#### Identificador técnico

O identificador do Tauri continua `com.comparaprecos.desktop`. A pasta de dados do Valora não depende dele; ele define a pasta do WebView2 e a chave de registro usada pelo instalador para atualizações e desinstalação. Como ainda não existe instalação distribuída, pode ser revisto sem migração até o primeiro instalador publicado; depois disso, mudá-lo exigiria migração.

#### Validado e pendente

Resultados da validação da **versão pública** (Valora 0.1.0), com os instaladores gerados a partir do código público. Antes do empacotamento passaram os testes do backend, os testes do frontend (com typecheck, lint e formatação) e o E2E completo.

A configuração inicial, o login e a navegação foram verificados pela API do backend empacotado, com os mesmos endpoints usados pelo frontend. A interface foi verificada por capturas da janela: a tela de configuração inicial aparece na primeira abertura e a tela de login na reabertura. O preenchimento dos formulários pela interface (cliques e digitação) não foi automatizado.

**Windows 11 Pro** (x64; sem Java, PostgreSQL, Node.js ou Docker no Windows; caminho do usuário com espaço). Instalador `Valora_0.1.0_x64-setup.exe` gerado por compilação cruzada no WSL e instalado em modo silencioso (`/S`), numa pasta de dados vazia:

- instalação por usuário, sem pedido de administrador. Publisher `Valora`; Java, PostgreSQL e backend em `%LOCALAPPDATA%\Programs\Valora\runtime\`; a pasta de dados não é criada pelo instalador;
- primeira abertura: criação do cluster, PostgreSQL, Java e Spring Boot. O Flyway aplicou as 8 migrações;
- configuração inicial (criação do ADMIN; uma segunda tentativa é recusada), login e consulta às telas principais;
- CORS: a origem `http://tauri.localhost` é aceita, e uma origem estranha é recusada;
- fechamento normal da janela: encerra todos os processos e o PostgreSQL é parado;
- reabertura: reaproveita o banco e as senhas, sem nova migração, e os dados persistem;
- queda forçada (`taskkill /F`): o Java é encerrado pelo Job Object, e a nova abertura reaproveita o PostgreSQL ativo, sem perda de dados;
- desinstalação silenciosa: remove o programa, o atalho e a chave de desinstalação, e preserva `%LOCALAPPDATA%\Valora` e a pasta do WebView2. A chave `HKCU\Software\Valora\Valora` (pasta de instalação e idioma, gravada pelo template do Tauri) permanece após a desinstalação.

**Linux: somente no WSL (Ubuntu 26.04, glibc 2.43, WSLg).** Pacote `Valora_0.1.0_amd64.deb` instalado com `dpkg -i` (dependências satisfeitas: no Ubuntu 26.04 o `libgtk-3-0t64` fornece `libgtk-3-0`). Programa em `/usr/bin/valora` e `/usr/lib/Valora/runtime/`. Os dados foram isolados em uma pasta temporária por `XDG_DATA_HOME`, e não na pasta padrão (`~/.local/share/Valora`). Passaram:

- primeira abertura: PostgreSQL, Java, Flyway (8 migrações) e senhas com permissão `600`;
- configuração inicial, login e consulta às telas principais;
- fechamento normal (`WM_DELETE_WINDOW`), com todos os processos encerrados e o PostgreSQL parado;
- reabertura com as mesmas senhas e dados persistidos;
- queda forçada (`kill -9`): o Java é encerrado junto com o app, e o PostgreSQL é reaproveitado na reabertura.

Depois dos testes, `dpkg -r valora` removeu o pacote, `/usr/bin/valora`, `/usr/lib/Valora` e o atalho do menu. Isso **não equivale a uma validação em Linux nativo**. O AppImage não foi gerado (ver acima).

**Identidade visual:** depois dessas validações, os instaladores foram gerados novamente com os ícones do Valora (aplicativo, instalador Windows e pacote Linux) e a logo nas telas de login e de configuração inicial. Não houve outra alteração. O `.deb` regerado foi aberto no WSL e exibiu a logo na configuração inicial. A instalação do `.exe` regerado não foi repetida.

Ainda pendente (não concluído):

- teste em outra máquina Windows limpa, e no Windows 10 sem WebView2;
- teste da interface com cliques e digitação nos instaladores públicos, além do que o E2E cobre na versão web;
- usuário com caracteres acentuados no caminho, antivírus de terceiros e Smart App Control em modo de avaliação (os instaladores não são assinados);
- mensagem nativa de erro na falha de inicialização (ex.: porta 8080 ocupada) com os instaladores públicos;
- teste em Linux nativo (desktop físico ou VM) e em outras distribuições;
- AppImage (ver acima);
- política de backup (previsto: `pg_dump` do banco `valora`);
- atualização de versão principal do PostgreSQL (`pg_upgrade` ou dump/restauração);
- remoção opcional dos dados na desinstalação, e da chave `HKCU\Software\Valora\Valora`;
- caminhos da máquina de build gravados nos executáveis (fontes das dependências Rust em `~/.cargo/registry`), que podem ser removidos com `--remap-path-prefix`;
- assinatura digital e atualização automática;
- decisão final sobre o identificador técnico.

---

## 19. Limites da Arquitetura do MVP

A arquitetura não deverá incluir inicialmente:

- integração automática com SEFAZ;
- certificado digital;
- microsserviços;
- infraestrutura de nuvem obrigatória;
- integração com ERP;
- inteligência artificial;
- serviços externos desnecessários.

A prioridade é construir uma aplicação funcional, compreensível e demonstrável dentro do prazo do projeto.
