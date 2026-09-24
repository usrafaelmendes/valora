import { readFile } from 'node:fs/promises';
import { expect, test, type Locator, type Page, type TestInfo } from '@playwright/test';
import {
  ADMIN,
  baseCnpj,
  cnpjFicticio,
  criarUsuario,
  entrar,
  EXECUCAO,
  menu,
  reais,
  sair,
  selecionar,
  tokenApi,
} from './apoio';

/**
 * Fluxo principal do MVP, ponta a ponta, com dados fictícios:
 * ADMIN prepara cadastros e a configuração do cenário → USER cria a cotação → comparação →
 * nova opção e nova comparação (histórico preservado) → download do CSV.
 *
 * Configuração do cenário (somente no banco temporário do E2E): a instalação começa sem regras
 * tributárias e com parâmetros não definidos. O ADMIN cadastra pela interface regras FICTÍCIAS
 * de teste (PIS/COFINS 6,35%, IPI do fabricante e do atacadista com fator 0,25, ICMS
 * interestadual pela alíquota da operação) e define os parâmetros com valores de teste.
 */

const PRODUTO = `Produto E2E ${EXECUCAO}`;
/** GTIN do item 1 da NF-e sintética de teste (backend/src/test/resources/nfe). */
const GTIN_NFE_SINTETICA = '4006381333931';
const FABRICANTE_A = { nome: `Fabricante A E2E ${EXECUCAO}`, uf: 'MG', tipo: 'Fabricante' };
const ATACADISTA_B = {
  nome: `Atacadista B E2E ${EXECUCAO}`,
  uf: 'RJ',
  tipo: 'Atacadista/revendedor',
};
const FABRICANTE_C = { nome: `Fabricante C E2E ${EXECUCAO}`, uf: 'PR', tipo: 'Fabricante' };
/** Emitente da NF-e sintética (CNPJ de exemplo, fictício). */
const EMITENTE_NFE = { nome: `Emitente NF-e E2E ${EXECUCAO}`, cnpj: '11222333000181', uf: 'PR' };

const USUARIO = { email: '', senha: '' };
const ids = { cotacao: 0, comparacaoInicial: 0, novaComparacao: 0 };

test.describe.configure({ mode: 'serial' });

async function capturar(page: Page, testInfo: TestInfo, nome: string) {
  await page.screenshot({ path: testInfo.outputPath(`${nome}.png`), fullPage: true });
}

async function cadastrarFornecedor(
  page: Page,
  fornecedor: { nome: string; uf: string; tipo: string },
  cnpj: string,
) {
  await page.getByRole('button', { name: 'Cadastrar fornecedor' }).first().click();
  const modal = page.getByRole('dialog', { name: 'Cadastrar fornecedor' });
  await modal.getByLabel(/^Razão social/).fill(fornecedor.nome);
  await modal.getByLabel(/^CNPJ/).fill(cnpj);
  await selecionar(modal, /^UF de emissão/, fornecedor.uf);
  await selecionar(modal, /^Tipo do fornecedor/, fornecedor.tipo);
  await modal.getByLabel(/^Prazo de pagamento base/).fill('28/56/84 dias');
  await modal.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(modal).toBeHidden();
  await expect(page.getByRole('table', { name: 'Fornecedores' })).toContainText(fornecedor.nome);
}

/** Regra tributária FICTÍCIA de teste, cadastrada pela interface como o ADMIN faria. */
async function cadastrarRegra(
  page: Page,
  regra: {
    nome: string;
    tributo: string;
    forma: string;
    aliquota?: string;
    fator?: string;
    tipoFornecedor?: string;
    abrangencia?: string;
  },
) {
  await page.getByRole('button', { name: 'Cadastrar regra' }).first().click();
  const modal = page.getByRole('dialog', { name: 'Cadastrar regra tributária' });
  await modal.getByLabel(/^Nome/).fill(regra.nome);
  await selecionar(modal, /^Tributo/, regra.tributo);
  await selecionar(modal, /^Forma de obtenção da alíquota/, regra.forma);
  if (regra.aliquota) {
    await modal.getByLabel(/^Alíquota \(%\)/).fill(regra.aliquota);
  }
  if (regra.fator) {
    await modal.getByLabel(/^Fator/).fill(regra.fator);
  }
  if (regra.tipoFornecedor) {
    await selecionar(modal, /^Tipo de fornecedor/, regra.tipoFornecedor);
  }
  if (regra.abrangencia) {
    await selecionar(modal, /^Operação \(abrangência\)/, regra.abrangencia);
  }
  await modal.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(modal).toBeHidden();
  await expect(page.getByRole('table', { name: 'Regras tributárias' })).toContainText(regra.nome);
}

async function definirParametro(page: Page, titulo: string, opcao: string) {
  await page.getByRole('button', { name: `Alterar ${titulo}` }).click();
  const modal = page.getByRole('dialog', { name: `Alterar: ${titulo}` });
  await selecionar(modal, /^Valor/, opcao);
  // O MultiSelect continua aberto após a escolha; a lista (em portal) cobre o botão Salvar.
  if (await page.getByRole('listbox').isVisible()) {
    await page.keyboard.press('Escape');
    await expect(page.getByRole('listbox')).toBeHidden();
  }
  await modal.getByRole('button', { name: 'Salvar' }).click();
  await expect(modal).toBeHidden();
}

async function preencherOpcao(opcao: Locator, fornecedor: string, condicao: string, cfop?: string) {
  await selecionar(opcao, /^Fornecedor/, opcaoFornecedor(fornecedor));
  await opcao.getByLabel(/^Condição de pagamento/).fill(condicao);
  await opcao.getByLabel(/^Valor dos produtos/).fill('200,00');
  await opcao.getByLabel(/^Alíquota de ICMS/).fill('11');
  await opcao.getByLabel(/^Alíquota de IPI/).fill('10');
  if (cfop) {
    await opcao.getByLabel(/^CFOP/).fill(cfop);
  }
}

function numeroDoTitulo(texto: string | null): number {
  const numero = /nº (\d+)/.exec(texto ?? '')?.[1];
  if (!numero) {
    throw new Error(`Número não encontrado em "${texto ?? ''}".`);
  }
  return Number(numero);
}

function linhas(page: Page, tabela: string) {
  return page.getByRole('table', { name: tabela }).locator('tbody tr');
}

/** Rótulo da opção no Select de fornecedor: "Razão social (CNPJ formatado)". */
function opcaoFornecedor(nome: string) {
  return new RegExp(`^${nome} \\(`);
}

test('ADMIN entra, cadastra produto e fornecedores, importa NF-e e configura o cenário', async ({
  page,
}, testInfo) => {
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Entrar' })).toBeVisible();
  await capturar(page, testInfo, '01-login');

  await entrar(page, ADMIN.email, ADMIN.senha);
  await expect(page.getByRole('heading', { name: 'Olá, Admin E2E' })).toBeVisible();
  await expect(page.getByLabel('Perfil de acesso')).toHaveText('Administrador');
  await expect(menu(page)).toContainText('Administração');
  await capturar(page, testInfo, '02-inicio-admin');

  // Produto
  await menu(page).getByRole('link', { name: 'Produtos' }).click();
  await page.getByRole('button', { name: 'Cadastrar produto' }).first().click();
  const modalProduto = page.getByRole('dialog', { name: 'Cadastrar produto' });
  await modalProduto.getByLabel(/^Nome/).fill(PRODUTO);
  await modalProduto.getByLabel(/^GTIN/).fill(GTIN_NFE_SINTETICA);
  await modalProduto.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(modalProduto).toBeHidden();
  await expect(page.getByRole('table', { name: 'Produtos' })).toContainText(PRODUTO);
  await capturar(page, testInfo, '03-produtos');

  // Fornecedores
  await menu(page).getByRole('link', { name: 'Fornecedores' }).click();
  await cadastrarFornecedor(page, FABRICANTE_A, cnpjFicticio(baseCnpj(1)));
  await cadastrarFornecedor(page, ATACADISTA_B, cnpjFicticio(baseCnpj(2)));
  await cadastrarFornecedor(page, FABRICANTE_C, cnpjFicticio(baseCnpj(3)));
  await cadastrarFornecedor(
    page,
    { nome: EMITENTE_NFE.nome, uf: EMITENTE_NFE.uf, tipo: 'Fabricante' },
    EMITENTE_NFE.cnpj,
  );
  await capturar(page, testInfo, '04-fornecedores');

  // NF-e sintética (upload manual do XML, somente ADMIN)
  await menu(page).getByRole('link', { name: 'NF-e' }).click();
  await page.getByRole('button', { name: 'Importar NF-e' }).first().click();
  const modalNfe = page.getByRole('dialog', { name: 'Importar NF-e' });
  await modalNfe
    .locator('input[type="file"]')
    .setInputFiles('../backend/src/test/resources/nfe/nfe-sintetica.xml');
  await modalNfe.getByRole('button', { name: 'Importar' }).click();
  const avisoNfe = page.getByRole('status').filter({ hasText: 'NF-e nº 1234 importada' });
  await expect(avisoNfe).toContainText('NF-e nº 1234 importada com 3 itens');
  await expect(page.getByRole('table', { name: 'NF-e importadas' })).toContainText(
    EMITENTE_NFE.nome,
  );
  await capturar(page, testInfo, '05-nfe');
  await avisoNfe.getByRole('link', { name: 'Ver detalhes' }).click();
  await expect(page.getByRole('heading', { name: 'NF-e nº 1234 (série 1)' })).toBeVisible();
  await page.getByRole('button', { name: /^Item 1:/ }).click();
  await expect(page.getByRole('table', { name: 'Tributos destacados no item 1' })).toBeVisible();
  await capturar(page, testInfo, '06-nfe-detalhe');

  // Regras tributárias: a instalação começa sem nenhuma; o ADMIN cadastra as regras fictícias.
  await menu(page).getByRole('link', { name: 'Regras tributárias' }).click();
  await expect(page.getByText('Nenhuma regra tributária cadastrada')).toBeVisible();
  const aliquotaDaNfe = 'Alíquota da NF-e';
  await cadastrarRegra(page, {
    nome: 'PIS/COFINS - crédito de teste (6,35%)',
    tributo: 'PIS/COFINS (combinado)',
    forma: 'Percentual fixo',
    aliquota: '6,35',
  });
  await cadastrarRegra(page, {
    nome: 'IPI - fornecedor fabricante (teste)',
    tributo: 'IPI',
    forma: aliquotaDaNfe,
    tipoFornecedor: 'Fabricante',
  });
  await cadastrarRegra(page, {
    nome: 'IPI - fornecedor atacadista (teste, fator 0,25)',
    tributo: 'IPI',
    forma: aliquotaDaNfe,
    fator: '0,25',
    tipoFornecedor: 'Atacadista/revendedor',
  });
  await cadastrarRegra(page, {
    nome: 'ICMS - operação interestadual (teste)',
    tributo: 'ICMS',
    forma: aliquotaDaNfe,
    abrangencia: 'Interestadual (origem diferente do destino)',
  });
  const regras = page.getByRole('table', { name: 'Regras tributárias' });
  await expect(regras.locator('tbody tr')).toHaveCount(4);
  await capturar(page, testInfo, '07-regras');

  // Parâmetros do cenário de teste (valores apenas para o E2E)
  await menu(page).getByRole('link', { name: 'Parâmetros de cálculo' }).click();
  await definirParametro(page, 'UF de destino', 'GO');
  const informado = 'Valores informados no cálculo (ex.: cotação) (INFORMADO)';
  await definirParametro(page, 'Fonte dos valores da operação', informado);
  await definirParametro(page, 'Fonte dos dados fiscais', informado);
  await definirParametro(
    page,
    'Fonte da UF de origem',
    'UF do cadastro do fornecedor (CADASTRO_FORNECEDOR)',
  );
  await definirParametro(
    page,
    'Composição do valor da operação',
    'Valor dos produtos (VALOR_PRODUTO)',
  );
  const parametros = page.getByRole('table', { name: 'Parâmetros de cálculo' });
  await expect(
    parametros.locator('tr', { hasText: 'Fonte dos valores da operação' }),
  ).toContainText('INFORMADO');
  await expect(
    parametros.locator('tr', { hasText: 'Composição do valor da operação' }),
  ).toContainText('Valor dos produtos');
  await capturar(page, testInfo, '08-parametros');

  await sair(page);
});

test('USER entra e consulta produtos e fornecedores, sem ações administrativas', async ({
  page,
  request,
}, testInfo) => {
  Object.assign(USUARIO, await criarUsuario(request, 'Usuario E2E'));
  await entrar(page, USUARIO.email, USUARIO.senha);
  await expect(page.getByLabel('Perfil de acesso')).toHaveText('Usuário');
  await expect(menu(page)).not.toContainText('Administração');

  await menu(page).getByRole('link', { name: 'Produtos' }).click();
  await expect(page.getByRole('table', { name: 'Produtos' })).toContainText(PRODUTO);
  await expect(page.getByRole('button', { name: 'Cadastrar produto' })).toHaveCount(0);

  await menu(page).getByRole('link', { name: 'Fornecedores' }).click();
  const fornecedores = page.getByRole('table', { name: 'Fornecedores' });
  await expect(fornecedores).toContainText(FABRICANTE_A.nome);
  await expect(fornecedores).toContainText(ATACADISTA_B.nome);
  await expect(page.getByRole('button', { name: 'Cadastrar fornecedor' })).toHaveCount(0);
  await capturar(page, testInfo, '09-fornecedores-user');
});

test('USER cria a cotação e vê a comparação inicial com rastreabilidade', async ({
  page,
}, testInfo) => {
  await entrar(page, USUARIO.email, USUARIO.senha);
  await menu(page).getByRole('link', { name: 'Cotações' }).click();
  await expect(page.getByText('Nenhuma cotação registrada')).toBeVisible();
  await capturar(page, testInfo, '10-cotacoes-vazia');
  await page.getByRole('link', { name: 'Nova cotação' }).first().click();

  const formulario = page.getByRole('form', { name: 'Nova cotação' });
  await selecionar(formulario, /^Produto/, PRODUTO);
  await formulario.getByLabel(/^Quantidade/).fill('2');
  await formulario.getByLabel(/^Descrição/).fill('Cotação E2E (dados fictícios)');

  await preencherOpcao(page.getByRole('region', { name: 'Opção 1' }), FABRICANTE_A.nome, '30 dias');
  await page.getByRole('button', { name: 'Adicionar outra opção' }).click();
  await preencherOpcao(page.getByRole('region', { name: 'Opção 2' }), ATACADISTA_B.nome, '60 dias');
  await page.getByRole('button', { name: 'Adicionar outra opção' }).click();
  // Opção com CFOP: CFOPS_PARTICIPANTES não está definido, então fica fora do ranking.
  await preencherOpcao(
    page.getByRole('region', { name: 'Opção 3' }),
    FABRICANTE_A.nome,
    '30 dias',
    '6102',
  );
  await capturar(page, testInfo, '11-nova-cotacao');
  await page.getByRole('button', { name: 'Criar cotação e comparar' }).click();

  const titulo = page.getByRole('heading', { name: /^Comparação nº \d+$/ });
  await expect(titulo).toBeVisible();
  ids.comparacaoInicial = numeroDoTitulo(await titulo.textContent());
  const aviso = page.getByRole('status').filter({ hasText: 'criada' });
  await expect(aviso).toContainText('Resultado da comparação inicial');
  ids.cotacao = numeroDoTitulo(await aviso.textContent());

  // Classificadas: valores calculados pelo backend com as regras fictícias cadastradas acima.
  const classificadas = linhas(page, 'Alternativas classificadas');
  await expect(classificadas).toHaveCount(2);
  const primeira = classificadas.nth(0);
  await expect(primeira).toContainText('1º');
  await expect(primeira).toContainText(FABRICANTE_A.nome);
  await expect(primeira).toContainText('Fabricante');
  await expect(primeira).toContainText('30 dias');
  await expect(primeira).toContainText(reais('200,00'));
  await expect(primeira).toContainText(/ICMS: R\$\s22,00/);
  await expect(primeira).toContainText(/IPI: R\$\s20,00/);
  await expect(primeira).toContainText(/PIS\/COFINS \(combinado\): R\$\s12,70/);
  await expect(primeira).toContainText(reais('54,70'));
  await expect(primeira).toContainText(reais('145,30'));
  const segunda = classificadas.nth(1);
  await expect(segunda).toContainText('2º');
  await expect(segunda).toContainText(ATACADISTA_B.nome);
  await expect(segunda).toContainText(/IPI: R\$\s5,00/);
  await expect(segunda).toContainText(reais('160,30'));
  await expect(page.getByRole('region', { name: 'Primeira posição' })).toContainText(
    FABRICANTE_A.nome,
  );

  // Não classificada: motivo do backend, sem posição.
  const naoClassificadas = linhas(page, 'Opções não classificadas');
  await expect(naoClassificadas).toHaveCount(1);
  await expect(naoClassificadas.first()).toContainText('CFOPs participantes não definidos');
  await expect(naoClassificadas.first()).toContainText('CFOP 6102');
  await expect(naoClassificadas.first()).not.toContainText(/\dº/);
  await capturar(page, testInfo, '12-comparacao');

  // Detalhes do cálculo: regras com versão, créditos individuais e parâmetros usados.
  await page
    .getByRole('button', { name: `Detalhes do cálculo: 1º lugar — ${FABRICANTE_A.nome}` })
    .click();
  const creditos = page.getByRole('table', { name: /^Créditos do cálculo \d+$/ });
  await expect(creditos.locator('tr', { hasText: 'IPI' })).toContainText(
    'IPI - fornecedor fabricante (teste) (v0)',
  );
  await expect(creditos.locator('tr', { hasText: 'PIS/COFINS' })).toContainText(
    'PIS/COFINS - crédito de teste (6,35%) (v0)',
  );
  await expect(creditos.locator('tr', { hasText: 'ICMS' })).toContainText('11%');
  await expect(page.getByText('Fonte dos valores da operação').first()).toBeVisible();
  await capturar(page, testInfo, '13-detalhes-calculo');

  const configuracao = page.getByRole('table', { name: 'Regras tributárias ativas na comparação' });
  await expect(
    configuracao.locator('tr', { hasText: 'ICMS - operação interestadual (teste)' }),
  ).toContainText('v0');
  await expect(page.getByText('CFOPs participantes (CFOPS_PARTICIPANTES)')).toBeVisible();
});

test('nova opção e nova comparação: novo registro e histórico preservado', async ({
  page,
}, testInfo) => {
  await entrar(page, USUARIO.email, USUARIO.senha);
  await page.goto(`/cotacoes/${String(ids.cotacao)}`);
  await expect(
    page.getByRole('heading', { name: `Cotação nº ${String(ids.cotacao)}` }),
  ).toBeVisible();

  await page.getByRole('button', { name: 'Adicionar opção' }).click();
  const modal = page.getByRole('dialog', { name: /^Adicionar opção à cotação/ });
  await preencherOpcao(modal, FABRICANTE_C.nome, '90 dias');
  await modal.getByRole('button', { name: 'Adicionar opção' }).click();
  await expect(modal).toBeHidden();
  // O backend devolve o id da opção criada (antes voltava null).
  await expect(
    page.getByRole('status').filter({ hasText: /Opção nº \d+ \(.*\) incluída na cotação/ }),
  ).toBeVisible();
  await expect(page.getByText('Nova comparação necessária')).toBeVisible();
  await expect(linhas(page, 'Opções da cotação').last()).toContainText('Não comparada');
  await capturar(page, testInfo, '14-cotacao-detalhe');

  await page.getByRole('button', { name: 'Nova comparação' }).click();
  const confirmacao = page.getByRole('dialog', { name: 'Executar nova comparação' });
  await confirmacao.getByRole('button', { name: 'Executar comparação' }).click();

  const titulo = page.getByRole('heading', { name: /^Comparação nº \d+$/ });
  await expect(titulo).not.toHaveText(`Comparação nº ${String(ids.comparacaoInicial)}`);
  ids.novaComparacao = numeroDoTitulo(await titulo.textContent());
  expect(ids.novaComparacao).toBeGreaterThan(ids.comparacaoInicial);
  await expect(
    page
      .getByRole('status')
      .filter({ hasText: `Nova comparação nº ${String(ids.novaComparacao)} executada` }),
  ).toBeVisible();

  // Fabricantes A e C com o mesmo custo: empate informado pelo backend.
  const classificadas = linhas(page, 'Alternativas classificadas');
  await expect(classificadas).toHaveCount(3);
  await expect(classificadas.nth(0)).toContainText(FABRICANTE_A.nome);
  await expect(classificadas.nth(0)).toContainText('Empate');
  await expect(classificadas.nth(1)).toContainText(FABRICANTE_C.nome);
  await expect(classificadas.nth(1)).toContainText('Empate');
  await expect(classificadas.nth(1)).toContainText(reais('145,30'));
  await expect(classificadas.nth(2)).toContainText('3º');
  await expect(classificadas.nth(2)).toContainText(ATACADISTA_B.nome);
  await capturar(page, testInfo, '15-nova-comparacao');

  // Histórico: as duas comparações, a anterior sem alteração.
  await page.getByRole('link', { name: 'Ver cotação' }).click();
  const historico = linhas(page, 'Histórico de comparações');
  await expect(historico).toHaveCount(2);
  await expect(historico.nth(0)).toContainText(`Nº ${String(ids.novaComparacao)}`);
  await expect(historico.nth(0)).toContainText('Mais recente');
  await historico
    .nth(1)
    .getByRole('link', { name: `Nº ${String(ids.comparacaoInicial)}` })
    .click();
  await expect(page.getByText('Existe uma comparação mais recente desta cotação')).toBeVisible();
  await expect(linhas(page, 'Alternativas classificadas')).toHaveCount(2);
});

test('download do CSV da comparação sem criar nova comparação', async ({
  page,
  request,
}, testInfo) => {
  await entrar(page, USUARIO.email, USUARIO.senha);
  await page.goto(`/comparacoes/${String(ids.novaComparacao)}`);
  await expect(
    page.getByRole('heading', { name: `Comparação nº ${String(ids.novaComparacao)}` }),
  ).toBeVisible();

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Baixar tabela (CSV)' }).click(),
  ]);
  expect(download.suggestedFilename()).toBe(
    `comparacao-${String(ids.novaComparacao)}-cotacao-${String(ids.cotacao)}.csv`,
  );
  const caminho = testInfo.outputPath(download.suggestedFilename());
  await download.saveAs(caminho);
  const bytes = await readFile(caminho);
  expect(bytes.subarray(0, 3)).toEqual(Buffer.from([0xef, 0xbb, 0xbf]));
  const conteudo = bytes.toString('utf8');
  expect(conteudo).toContain('Comparação de fornecedores');
  expect(conteudo).toContain(`Comparação;${String(ids.novaComparacao)}`);
  expect(conteudo).toContain(FABRICANTE_A.nome);
  expect(conteudo).toContain(ATACADISTA_B.nome);
  expect(conteudo).toContain('145,30');
  await expect(page.getByRole('alert')).toHaveCount(0);

  const token = await tokenApi(request, USUARIO.email, USUARIO.senha);
  const resposta = await request.get(`/api/cotacoes/${String(ids.cotacao)}/comparacoes`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const historico = (await resposta.json()) as unknown[];
  expect(historico).toHaveLength(2);
});

test('telas em largura de celular não têm rolagem horizontal na página', async ({
  page,
}, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await entrar(page, USUARIO.email, USUARIO.senha);
  for (const [caminho, nome] of [
    [`/comparacoes/${String(ids.novaComparacao)}`, '16-comparacao-celular'],
    [`/cotacoes/${String(ids.cotacao)}`, '17-cotacao-celular'],
    ['/cotacoes/nova', '18-nova-cotacao-celular'],
  ] as const) {
    await page.goto(caminho);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await page.waitForLoadState('networkidle');
    const larguraExcedente = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(larguraExcedente, `rolagem horizontal em ${caminho}`).toBeLessThanOrEqual(0);
    await capturar(page, testInfo, nome);
  }
});
