import { defineConfig, devices } from '@playwright/test';

/**
 * Testes E2E do sistema completo: PostgreSQL + backend (jar) + frontend (Vite com proxy /api).
 *
 * Execute pelo script `npm run e2e` (e2e/executar.sh): ele sobe o PostgreSQL, cria um banco
 * temporário exclusivo e vazio, gera segredo JWT e senha do ADMIN aleatórios e remove o banco no
 * fim. O banco de desenvolvimento nunca é usado.
 *
 * O backend inicia sem nenhum usuário: o projeto "configuracao-inicial" roda primeiro e cria o
 * ADMIN pela tela de primeiro acesso; os demais testes dependem dele.
 */
const variaveis = [
  'E2E_JAR',
  'E2E_POSTGRES_DB',
  'E2E_JWT_SECRET',
  'E2E_ADMIN_EMAIL',
  'E2E_ADMIN_SENHA',
] as const;

const faltando = variaveis.filter((nome) => !process.env[nome]);
if (faltando.length > 0) {
  throw new Error(
    `Variáveis ausentes: ${faltando.join(', ')}. Execute os testes E2E com "npm run e2e".`,
  );
}

const ambiente = process.env as Record<(typeof variaveis)[number], string> & NodeJS.ProcessEnv;
const FRONTEND_URL = 'http://localhost:5173';

export default defineConfig({
  testDir: './e2e',
  // O fluxo principal é sequencial e compartilha o mesmo banco: um worker, sem paralelismo.
  workers: 1,
  fullyParallel: false,
  // Falha intermitente não é aceita: sem novas tentativas automáticas.
  retries: 0,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: [['list']],
  use: {
    baseURL: FRONTEND_URL,
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'configuracao-inicial',
      testMatch: /configuracao-inicial\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'chromium',
      testIgnore: /configuracao-inicial\.spec\.ts/,
      dependencies: ['configuracao-inicial'],
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: `java -jar "${ambiente.E2E_JAR}"`,
      cwd: '../backend',
      url: 'http://localhost:8080/auth/me',
      reuseExistingServer: false,
      timeout: 120_000,
      env: {
        ...ambiente,
        POSTGRES_DB: ambiente.E2E_POSTGRES_DB,
        JWT_SECRET: ambiente.E2E_JWT_SECRET,
      },
    },
    {
      command: 'npx vite --port 5173 --strictPort',
      url: FRONTEND_URL,
      reuseExistingServer: false,
      timeout: 60_000,
    },
  ],
});
