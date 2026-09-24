import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test';

/**
 * Apoio dos testes E2E. Todos os dados são fictícios: nomes, e-mails, CNPJs (gerados com
 * dígitos verificadores válidos) e valores. Nenhum dado real de empresa ou NF-e.
 */

/** Criado pela tela de configuração inicial (configuracao-inicial.spec.ts), no banco vazio. */
export const ADMIN = {
  nome: 'Admin E2E',
  email: process.env.E2E_ADMIN_EMAIL ?? '',
  senha: process.env.E2E_ADMIN_SENHA ?? '',
};

/** Sufixo único por execução, para os cadastros não colidirem se o banco for mantido. */
export const EXECUCAO = Date.now().toString(36).toUpperCase();

/** CNPJ numérico fictício com dígitos verificadores válidos, a partir de 12 dígitos. */
export function cnpjFicticio(base12: string): string {
  const digito = (numeros: string) => {
    const pesos =
      numeros.length === 12
        ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
        : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    const soma = Array.from(numeros, Number).reduce(
      (total, n, i) => total + n * (pesos[i] ?? 0),
      0,
    );
    const resto = soma % 11;
    return resto < 2 ? '0' : String(11 - resto);
  };
  const primeiro = digito(base12);
  return base12 + primeiro + digito(base12 + primeiro);
}

/** Base de 12 dígitos derivada da execução, para CNPJs diferentes a cada rodada. */
export function baseCnpj(indice: number): string {
  const numero = (Date.now() % 1_000_000_000) * 10 + indice;
  return String(numero).padStart(12, '9').slice(-12);
}

/** Valor em reais como exibido (o separador após "R$" é um espaço não separável). */
export function reais(valor: string): RegExp {
  return new RegExp(`R\\$\\s${valor.replace('.', '\\.')}`);
}

export async function entrar(page: Page, email: string, senha: string) {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  await page.getByRole('button', { name: 'Entrar' }).click();
  // Após o login a aplicação volta à página pedida (ou ao início): o layout autenticado aparece.
  await expect(page.getByRole('button', { name: 'Sair' })).toBeVisible();
}

export async function sair(page: Page) {
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
}

/** Escolhe uma opção de Select/MultiSelect do Mantine (a lista abre em um portal). */
export async function selecionar(escopo: Locator | Page, rotulo: RegExp, opcao: string | RegExp) {
  const pagina = 'page' in escopo ? escopo.page() : escopo;
  await escopo.getByRole('combobox', { name: rotulo }).click();
  await pagina
    .getByRole('option', typeof opcao === 'string' ? { name: opcao, exact: true } : { name: opcao })
    .click();
}

export function menu(page: Page) {
  return page.getByRole('navigation', { name: 'Menu principal' });
}

/** Token do ADMIN pela API (somente para preparar o que a interface não oferece). */
export async function tokenApi(request: APIRequestContext, email: string, senha: string) {
  const resposta = await request.post('/api/auth/login', { data: { email, senha } });
  expect(resposta.status()).toBe(200);
  const corpo = (await resposta.json()) as { token: string };
  return corpo.token;
}

/**
 * Cria um USER fictício pela API (POST /usuarios, exclusivo do ADMIN), para preparar os demais
 * testes. O cadastro pela tela Usuários é coberto em usuarios.spec.ts. Retorna as credenciais.
 */
export async function criarUsuario(request: APIRequestContext, nome: string) {
  const token = await tokenApi(request, ADMIN.email, ADMIN.senha);
  const usuario = {
    nome,
    email: `${nome.toLowerCase().replace(/\W+/g, '.')}.${EXECUCAO.toLowerCase()}@teste.local`,
    senha: `Senha-${EXECUCAO}-${String(Math.random()).slice(2, 10)}`,
    perfil: 'USER',
  };
  const resposta = await request.post('/api/usuarios', {
    data: usuario,
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(resposta.status()).toBe(201);
  return { email: usuario.email, senha: usuario.senha };
}
