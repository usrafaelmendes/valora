import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { ADMIN, entrar, EXECUCAO, menu, sair, selecionar, tokenApi } from './apoio';

/**
 * Gerenciamento de usuários pela interface, contra o backend real. Depende do projeto
 * "configuracao-inicial": o banco temporário começa vazio e o primeiro ADMIN é criado pela
 * configuração inicial. Aqui o ADMIN cadastra um USER pela tela Usuários; o USER entra, não
 * acessa a área e recebe 403 da API. Dados fictícios, gerados a cada execução.
 */

test.describe.configure({ mode: 'serial' });

const NOVO_USER = {
  nome: `Usuario Tela E2E ${EXECUCAO}`,
  email: `usuario.tela.${EXECUCAO.toLowerCase()}@teste.local`,
  senha: `Senha-${EXECUCAO}-${String(Math.random()).slice(2, 10)}`,
};

async function capturar(page: Page, testInfo: TestInfo, nome: string) {
  await page.screenshot({ path: testInfo.outputPath(`${nome}.png`), fullPage: true });
}

async function preencherCadastro(page: Page, email: string) {
  const modal = page.getByRole('dialog', { name: 'Cadastrar usuário' });
  await modal.getByLabel(/^Nome/).fill(NOVO_USER.nome);
  await modal.getByLabel(/^E-mail/).fill(email);
  await modal.getByLabel(/^Senha/).fill(NOVO_USER.senha);
  await modal.getByLabel(/^Confirmar senha/).fill(NOVO_USER.senha);
  await selecionar(modal, /^Perfil/, 'Usuário (USER)');
  return modal;
}

test('ADMIN acessa Usuários pelo menu e cadastra um USER', async ({ page }, testInfo) => {
  await entrar(page, ADMIN.email, ADMIN.senha);
  await menu(page).getByRole('link', { name: 'Usuários' }).click();
  await expect(page.getByRole('heading', { name: 'Usuários' })).toBeVisible();
  const tabela = page.getByRole('table', { name: 'Usuários' });
  await expect(tabela).toContainText(ADMIN.email);

  await page.getByRole('button', { name: 'Cadastrar usuário' }).first().click();
  const modal = page.getByRole('dialog', { name: 'Cadastrar usuário' });
  // Validação da interface: nada é enviado.
  await modal.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(modal.getByText('O perfil é obrigatório (ADMIN ou USER).')).toBeVisible();

  // E-mail já cadastrado (o do ADMIN): o backend responde 409 e o formulário continua aberto.
  await preencherCadastro(page, ADMIN.email.toUpperCase());
  await modal.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(modal.getByRole('alert')).toContainText(
    'Já existe um usuário cadastrado com este e-mail.',
  );
  await capturar(page, testInfo, '01-usuarios-email-duplicado');

  await modal.getByLabel(/^E-mail/).fill(NOVO_USER.email);
  await modal.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(modal).toBeHidden();
  await expect(page.getByRole('status')).toContainText(
    `Usuário "${NOVO_USER.nome}" cadastrado com o perfil Usuário.`,
  );
  const linha = tabela.getByRole('row', { name: new RegExp(NOVO_USER.email) });
  await expect(linha).toContainText('Usuário');
  await expect(linha).toContainText('Ativo');
  await expect(page.locator('body')).not.toContainText(NOVO_USER.senha);
  await capturar(page, testInfo, '02-usuarios-desktop');

  // Largura de celular: a tabela rola dentro do próprio contêiner, nunca a página.
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/usuarios');
  await expect(tabela).toContainText(NOVO_USER.email);
  await page.waitForLoadState('networkidle');
  const larguraExcedente = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(larguraExcedente, 'rolagem horizontal em /usuarios').toBeLessThanOrEqual(0);
  await capturar(page, testInfo, '03-usuarios-390px');
  await page.getByRole('button', { name: 'Cadastrar usuário' }).first().click();
  await expect(modal).toBeVisible();
  await capturar(page, testInfo, '04-usuarios-formulario-390px');
});

test('USER entra, não vê Usuários e recebe "Acesso restrito" e 403', async ({ page, request }) => {
  await entrar(page, NOVO_USER.email, NOVO_USER.senha);
  await expect(page.getByRole('heading', { name: `Olá, ${NOVO_USER.nome}` })).toBeVisible();
  await expect(menu(page)).not.toContainText('Usuários');

  await page.goto('/usuarios');
  await expect(page.getByRole('heading', { name: 'Acesso restrito' })).toBeVisible();
  await expect(page.getByRole('table', { name: 'Usuários' })).toHaveCount(0);

  const token = await tokenApi(request, NOVO_USER.email, NOVO_USER.senha);
  const comToken = { headers: { Authorization: `Bearer ${token}` } };
  expect((await request.get('/api/usuarios', comToken)).status()).toBe(403);
  const criar = await request.post('/api/usuarios', {
    ...comToken,
    data: {
      nome: 'Negado E2E',
      email: `negado.${EXECUCAO.toLowerCase()}@teste.local`,
      senha: NOVO_USER.senha,
      perfil: 'ADMIN',
    },
  });
  expect(criar.status()).toBe(403);
  expect((await request.get('/api/nfe', comToken)).status()).toBe(403);
});

test('ADMIN continua acessando Usuários normalmente', async ({ page }) => {
  await entrar(page, ADMIN.email, ADMIN.senha);
  await page.goto('/usuarios');
  const tabela = page.getByRole('table', { name: 'Usuários' });
  await expect(tabela).toContainText(ADMIN.email);
  await expect(tabela).toContainText(NOVO_USER.email);
  // O USER negado acima não foi criado.
  await expect(tabela).not.toContainText(`negado.${EXECUCAO.toLowerCase()}`);
  await sair(page);
});
