import { expect, test } from '@playwright/test';
import { ADMIN, entrar, sair } from './apoio';

/**
 * Primeiro acesso em uma instalação nova (banco temporário vazio, sem ADMIN_* no ambiente):
 * a aplicação leva à configuração inicial, o ADMIN é criado pela interface e, depois disso,
 * a configuração inicial deixa de estar disponível. Os demais testes E2E dependem deste.
 */

test.describe.configure({ mode: 'serial' });

const URL_API = '/api/auth/configuracao-inicial';

test('banco vazio: a API informa que o sistema não está configurado', async ({ request }) => {
  const resposta = await request.get(URL_API);
  expect(resposta.status()).toBe(200);
  expect(await resposta.json()).toEqual({ configurado: false });
});

test('primeiro acesso: cria o ADMIN pela interface, entra e não volta à configuração', async ({
  page,
}, testInfo) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/configuracao-inicial$/);
  await expect(page.getByRole('heading', { name: 'Configuração inicial' })).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath('00-configuracao-inicial.png'),
    fullPage: true,
  });

  await page.getByLabel(/^Nome/).fill(ADMIN.nome);
  await page.getByLabel(/^E-mail/).fill(ADMIN.email);
  await page.getByLabel(/^Senha/).fill(ADMIN.senha);
  await page.getByLabel(/^Confirmar senha/).fill(`${ADMIN.senha}-diferente`);
  await page.getByRole('button', { name: 'Criar administrador' }).click();
  await expect(page.getByText('A confirmação da senha não confere.')).toBeVisible();

  await page.getByLabel(/^Confirmar senha/).fill(ADMIN.senha);
  await page.getByRole('button', { name: 'Criar administrador' }).click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByText('Administrador criado. Entre com o e-mail e a senha cadastrados.'),
  ).toBeVisible();

  await entrar(page, ADMIN.email, ADMIN.senha);
  await expect(page.getByRole('heading', { name: `Olá, ${ADMIN.nome}` })).toBeVisible();
  await expect(page.getByText('Administrador', { exact: true })).toBeVisible();

  // Com sessão, a configuração inicial leva ao início; sem sessão, ao login.
  await page.goto('/configuracao-inicial');
  await expect(page.getByRole('heading', { name: `Olá, ${ADMIN.nome}` })).toBeVisible();
  await expect(page).not.toHaveURL(/configuracao-inicial/);

  await sair(page);
  await page.goto('/configuracao-inicial');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
});

test('depois de configurado, a API não aceita criar outro ADMIN', async ({ request }) => {
  const status = await request.get(URL_API);
  expect(await status.json()).toEqual({ configurado: true });

  const outro = {
    nome: 'Outro Admin E2E',
    email: 'outro.admin.e2e@teste.local',
    senha: `${ADMIN.senha}-outro`,
  };
  const resposta = await request.post(URL_API, {
    data: { ...outro, confirmacaoSenha: outro.senha },
  });
  expect(resposta.status()).toBe(409);

  const login = await request.post('/api/auth/login', {
    data: { email: outro.email, senha: outro.senha },
  });
  expect(login.status()).toBe(401);
});
