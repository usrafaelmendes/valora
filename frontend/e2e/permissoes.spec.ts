import { expect, test, type APIRequestContext } from '@playwright/test';
import { ADMIN, criarUsuario, entrar, menu, tokenApi } from './apoio';

/**
 * Autenticação e autorização contra o backend real (via proxy /api do frontend). As
 * permissões verificadas são as do SecurityConfig: nada é ajustado para o teste passar.
 */

test.describe.configure({ mode: 'serial' });

const tokens = { admin: '', user: '' };
const credenciaisUser = { email: '', senha: '' };

function comToken(token: string) {
  return { headers: { Authorization: `Bearer ${token}` } };
}

async function status(
  request: APIRequestContext,
  metodo: 'get' | 'post' | 'put' | 'delete',
  caminho: string,
  token?: string,
  data?: unknown,
) {
  const opcoes = { ...(token ? comToken(token) : {}), ...(data === undefined ? {} : { data }) };
  const resposta = await request[metodo](`/api${caminho}`, opcoes);
  return resposta.status();
}

test.beforeAll(async ({ request }) => {
  tokens.admin = await tokenApi(request, ADMIN.email, ADMIN.senha);
  Object.assign(credenciaisUser, await criarUsuario(request, 'Usuario Permissoes E2E'));
  tokens.user = await tokenApi(request, credenciaisUser.email, credenciaisUser.senha);
});

test('sem token ou com token inválido a API responde 401', async ({ request }) => {
  for (const caminho of [
    '/cotacoes',
    '/fornecedores',
    '/produtos',
    '/regras-tributarias',
    '/auth/me',
  ]) {
    expect(await status(request, 'get', caminho), caminho).toBe(401);
  }
  expect(await status(request, 'get', '/cotacoes', 'token.invalido.teste')).toBe(401);
  const login = await request.post('/api/auth/login', {
    data: { email: ADMIN.email, senha: 'senha-incorreta-e2e' },
  });
  expect(login.status()).toBe(401);
});

test('USER consulta cadastros e usa cotações; operações administrativas dão 403', async ({
  request,
}) => {
  for (const caminho of [
    '/fornecedores',
    '/produtos',
    '/regras-tributarias',
    '/parametros-calculo',
    '/cotacoes',
  ]) {
    expect(await status(request, 'get', caminho, tokens.user), caminho).toBe(200);
  }

  const negadas: [Parameters<typeof status>[1], string, unknown?][] = [
    ['post', '/produtos', { nome: 'Produto negado E2E', descricao: null, gtin: null }],
    ['post', '/fornecedores', {}],
    ['put', '/parametros-calculo/UF_DESTINO', { valor: 'GO' }],
    ['post', '/regras-tributarias', {}],
    ['put', '/regras-tributarias/1', {}],
    ['delete', '/regras-tributarias/1'],
    ['get', '/nfe'],
    ['get', '/usuarios'],
  ];
  for (const [metodo, caminho, corpo] of negadas) {
    expect(await status(request, metodo, caminho, tokens.user, corpo), `${metodo} ${caminho}`).toBe(
      403,
    );
  }

  const upload = await request.post('/api/nfe', {
    ...comToken(tokens.user),
    multipart: {
      arquivo: { name: 'negado.xml', mimeType: 'text/xml', buffer: Buffer.from('<nfeProc/>') },
    },
  });
  expect(upload.status()).toBe(403);
});

test('ADMIN acessa as operações administrativas', async ({ request }) => {
  for (const caminho of ['/nfe', '/usuarios', '/regras-tributarias', '/parametros-calculo']) {
    expect(await status(request, 'get', caminho, tokens.admin), caminho).toBe(200);
  }
  // XML inválido é rejeitado pela validação (e não pela autorização).
  const upload = await request.post('/api/nfe', {
    ...comToken(tokens.admin),
    multipart: {
      arquivo: { name: 'invalido.xml', mimeType: 'text/xml', buffer: Buffer.from('<nao-e-nfe/>') },
    },
  });
  expect([400, 422]).toContain(upload.status());
});

test('USER não vê o menu administrativo e recebe "Acesso restrito" pela URL', async ({ page }) => {
  await entrar(page, credenciaisUser.email, credenciaisUser.senha);
  await expect(menu(page)).not.toContainText('Administração');
  for (const caminho of ['/nfe', '/regras-tributarias', '/parametros-calculo']) {
    await page.goto(caminho);
    await expect(page.getByRole('heading', { name: 'Acesso restrito' })).toBeVisible();
  }
});

test('sessão com token inválido volta ao login (401) e a rota protegida exige login', async ({
  page,
}) => {
  await page.goto('/cotacoes');
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();

  await entrar(page, credenciaisUser.email, credenciaisUser.senha);
  await page.evaluate(() => {
    const chave = 'compara-precos.sessao';
    const sessao = JSON.parse(sessionStorage.getItem(chave) ?? '{}') as Record<string, unknown>;
    sessionStorage.setItem(chave, JSON.stringify({ ...sessao, token: 'token.invalido.e2e' }));
  });
  await page.goto('/cotacoes');
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
});
