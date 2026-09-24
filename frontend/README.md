# Frontend — Valora

SPA em React 19 + TypeScript + Vite, com Mantine, React Router e TanStack Query.
Decisões registradas em `docs/ARQUITETURA.md` §18.1.

## Executar em desenvolvimento

Pré-requisitos: Node.js 20.19+ e o backend rodando em `http://localhost:8080`.

```bash
npm install
npm run dev        # http://localhost:5173
```

O Vite repassa `/api/*` para o backend (sem o prefixo `/api`), na mesma origem para o navegador.

### Versão desktop (Tauri, desenvolvimento)

`src-tauri/` contém o shell desktop do Tauri 2 (Valora desktop): uma janela que carrega este mesmo
frontend (sem comandos, plugins ou permissões de APIs do Tauri para o frontend). Requer Rust e as
bibliotecas de sistema do Tauri (ver README da raiz). Em desenvolvimento, o backend continua sendo
iniciado separadamente.

No desktop o frontend é gerado no modo `desktop` do Vite e chama o backend local diretamente
(`http://localhost:8080`, definido em `vite.config.ts`), sem o proxy `/api`; o backend libera CORS
somente para as origens do Tauri e do Vite (`docs/ARQUITETURA.md` §18.1).

```bash
npm run tauri dev     # sobe o Vite (npm run dev:desktop) e abre a janela "Valora"
npm run tauri build   # build de produção (npm run build:desktop) e executável em src-tauri/target/release/
```

A configuração base (`src-tauri/tauri.conf.json`) não gera instalador (`bundle.active: false`).

### Pacotes do Valora desktop (Windows e Linux)

No app distribuído, o próprio Tauri inicia o PostgreSQL e o backend locais a partir de `runtime/`
empacotada; sem essa pasta, como no desenvolvimento, nada é iniciado. Os runtimes são montados por
plataforma em `src-tauri/runtime/<plataforma>/` (ignorada pelo Git), e cada plataforma tem sua
configuração de empacotamento (`src-tauri/tauri.empacotamento.<plataforma>.json`):

```bash
npm run runtime:windows && npm run empacotar:windows   # instalador NSIS (Valora_*_x64-setup.exe)
npm run runtime:linux && npm run empacotar:linux       # pacote .deb (Valora_*_amd64.deb)
```

O código específico de cada sistema operacional fica em `src-tauri/src/plataforma.rs`. Requisitos da
máquina de build, estrutura dos pacotes e validação: `docs/ARQUITETURA.md` §18.2.

## Scripts

| Comando                                         | O que faz                                         |
| ----------------------------------------------- | ------------------------------------------------- |
| `npm test`                                      | Testes (Vitest + React Testing Library)           |
| `npm run typecheck`                             | Verificação de tipos (TypeScript strict)          |
| `npm run lint`                                  | ESLint                                            |
| `npm run format` / `format:check`               | Prettier                                          |
| `npm run build`                                 | Build de produção em `dist/`                      |
| `npm run e2e`                                   | Testes E2E do sistema completo (Playwright)       |
| `npm run dev:desktop`                           | Vite no modo desktop (API direta, sem proxy)      |
| `npm run build:desktop`                         | Build do modo desktop em `dist/`                  |
| `npm run tauri dev`                             | Janela desktop (Tauri) em desenvolvimento         |
| `npm run tauri build`                           | Executável desktop de produção (sem instalador)   |
| `npm run runtime:windows` / `runtime:linux`     | Monta os runtimes do pacote da plataforma         |
| `npm run empacotar:windows` / `empacotar:linux` | Instalador NSIS (Windows) / pacote `.deb` (Linux) |

## Estrutura de `src/`

| Pasta         | Conteúdo                                                                             |
| ------------- | ------------------------------------------------------------------------------------ |
| `api/`        | Cliente HTTP central (`httpClient.ts`), `ApiError` e funções por recurso da API      |
| `app/`        | Composição da aplicação: provedores e `QueryClient`                                  |
| `auth/`       | Sessão (token em `sessionStorage`), contexto, `useAuth` e guardas de rota            |
| `components/` | Componentes reutilizáveis (cabeçalho, carregamento, erro, vazio, tabela, modais)     |
| `config/`     | Configuração (URL base da API, tema do Mantine)                                      |
| `hooks/`      | Hooks reutilizáveis (título da página, estado de formulário)                         |
| `layouts/`    | Layout das páginas autenticadas (cabeçalho, menu por perfil)                         |
| `pages/`      | Páginas (uma por rota); áreas com formulário têm pasta própria (`produtos/`, `nfe/`) |
| `routes/`     | Caminhos, mapa de rotas e menu por perfil                                            |
| `types/`      | Tipos dos contratos da API (espelham os DTOs do backend)                             |
| `utils/`      | Funções utilitárias sem estado                                                       |
| `test/`       | Configuração e utilitários de teste (dados fictícios)                                |

Os testes ficam ao lado do código testado (`*.test.ts(x)`).

## Padrão das telas de cadastro

Produtos e Fornecedores definem o padrão para as próximas áreas:

- listagem com `useQuery` e `ResultadoConsulta` (carregando, erro com "Tentar novamente", vazio);
- `TabelaDados` para a tabela e `EstadoVazio` para a lista sem registros;
- cadastro/edição em `FormularioModal` com `useFormulario`: a validação local espelha o DTO
  do backend, e os erros por campo devolvidos pela API aparecem no próprio campo;
- ações destrutivas confirmadas em `ConfirmacaoAcao`;
- após cada mutação, a consulta da listagem é invalidada (`queryClient.invalidateQueries`);
- ações de escrita aparecem somente para o ADMIN; o backend continua sendo quem autoriza.

## Áreas administrativas (somente ADMIN)

| Rota                             | Endpoints                                                             |
| -------------------------------- | --------------------------------------------------------------------- |
| `/nfe`, `/nfe/:id`               | `GET /nfe`, `GET /nfe/{id}`, `POST /nfe` (multipart, `arquivo`)       |
| `/regras-tributarias`            | `GET/POST /regras-tributarias`, `PUT/DELETE /regras-tributarias/{id}` |
| `/regras-tributarias/aplicaveis` | `POST /regras-tributarias/aplicaveis`                                 |
| `/parametros-calculo`            | `GET /parametros-calculo`, `PUT /parametros-calculo/{chave}`          |

- NF-e: upload manual do XML (sem SEFAZ nem certificado). O frontend só confere extensão,
  arquivo vazio e o limite de 1 MB (o mesmo do backend); o conteúdo é validado pelo backend.
  Os detalhes mostram os valores do XML como vieram, sem recalcular impostos.
- Regras tributárias: cadastro, edição (cada alteração gera nova `versao`), desativação lógica
  e conferência das regras aplicáveis. O frontend não sugere alíquotas nem decide qual regra
  vale: `APLICAVEL`, `SEM_REGRA` e `CONFLITO` vêm do backend.
- Parâmetros de cálculo: as chaves e os valores aceitos vêm do backend. Parâmetro sem valor
  aparece como "não definido" e nunca recebe valor presumido.

## Cotações e comparações (ADMIN e USER)

| Rota               | Endpoints                                                                      |
| ------------------ | ------------------------------------------------------------------------------ |
| `/cotacoes`        | `GET /cotacoes` (filtro opcional `produtoId`)                                  |
| `/cotacoes/nova`   | `POST /cotacoes` (o backend já executa a comparação inicial)                   |
| `/cotacoes/:id`    | `GET /cotacoes/{id}`, `POST /cotacoes/{id}/opcoes`, `GET/POST .../comparacoes` |
| `/comparacoes/:id` | `GET /comparacoes/{id}`, `GET /comparacoes/{id}/download`                      |

- A comparação é exibida como gravada: alternativas classificadas na ordem recebida (posição e
  empate do backend) e não classificadas em seção separada, com situação e motivo, sem posição
  e sem custo zero presumido. Os detalhes de cada cálculo (parâmetros, valores, alíquotas,
  créditos com regra e versão, arredondamento e pendências) e a configuração usada ficam visíveis.
- Incluir uma opção não executa comparação: a cotação recarregada informa
  (`todasAsOpcoesComparadas`) quando é preciso uma nova. "Nova comparação" pede confirmação,
  cria um novo registro e o abre; as anteriores continuam no histórico.
- O download baixa o CSV montado pelo backend, com o nome de `Content-Disposition`.
- Valores e dados fiscais da opção são opcionais: campo em branco é enviado como `null` (não
  informado, nunca zero). A escolha de item de NF-e aparece só para o ADMIN, pois `GET /nfe` é
  restrito a ele no backend.

O frontend não contém regras tributárias nem cálculos: apenas apresenta o que a API retorna.

## Testes E2E (Playwright)

Ficam em `e2e/` e rodam contra o sistema completo: PostgreSQL + backend + frontend.

```bash
npx playwright install chromium   # somente na primeira vez
npm run e2e                       # ou: npm run e2e -- --grep "download"
```

O script `e2e/executar.sh`:

- sobe o PostgreSQL do `compose.yaml` (credenciais do `.env` da raiz) e cria um banco temporário
  exclusivo (`valora_e2e`, nunca o de desenvolvimento), removido ao final;
- empacota o backend e gera segredo JWT e senha do ADMIN aleatórios, válidos só na execução;
- o banco começa vazio: o projeto `configuracao-inicial` roda primeiro e cria o ADMIN pela tela de
  configuração inicial; os demais testes dependem dele;
- executa o Playwright, que inicia o backend (jar, porta 8080) e o Vite (porta 5173).

Os dados são fictícios (CNPJs gerados, XML sintético de `backend/src/test/resources/nfe`). Os
parâmetros necessários recebem valores de teste e o cenário cadastra regras tributárias fictícias pela
interface, somente no banco temporário (uma instalação nova começa sem regras). Resultados, traces e capturas de
tela ficam em `test-results/` (ignorado pelo Git).
