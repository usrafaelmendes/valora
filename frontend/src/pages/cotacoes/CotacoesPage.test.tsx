import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import {
  COMPARACAO_RESUMO_TESTE,
  COMPARACAO_TESTE,
  COTACAO_RESUMO_TESTE,
  COTACAO_TESTE,
  FORNECEDOR_TESTE,
  NFE_RESUMO_TESTE,
  NFE_TESTE,
  PRODUTO_TESTE,
} from '../../test/dadosFicticios';
import {
  corpoEnviado,
  instalarFetchFalso,
  respostaAdiada,
  respostaJson,
  respostaProblema,
  USUARIO_ADMIN_TESTE,
  USUARIO_USER_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { selecionar } from '../../test/selecionar';
import { salvarSessaoTeste } from '../../test/sessao';

const OUTRO_FORNECEDOR = { ...FORNECEDOR_TESTE, id: 11, razaoSocial: 'Outro Fornecedor Ficticio' };

function chamadas(fetchFalso: ReturnType<typeof instalarFetchFalso>, metodo: string) {
  return fetchFalso.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === metodo);
}

describe('CotacoesPage — listagem', () => {
  it('lista as cotações com os dados do backend e o acesso aos detalhes', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/cotacoes': () => respostaJson([COTACAO_RESUMO_TESTE]),
      'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
    });
    renderizarApp('/cotacoes');

    const tabela = await screen.findByRole('table', { name: 'Cotações registradas' });
    const linha = within(tabela).getAllByRole('row')[1];
    expect(linha).toHaveTextContent('Nº 3');
    expect(linha).toHaveTextContent('Produto Ficticio A');
    expect(linha).toHaveTextContent('2');
    expect(linha).toHaveTextContent('Cotação de teste');
    expect(linha).toHaveTextContent('20/09/2026, 11:59');
    expect(within(tabela).getByRole('link', { name: 'Ver detalhes da cotação 3' })).toHaveAttribute(
      'href',
      '/cotacoes/3',
    );
  });

  it('mostra o carregamento e depois o estado vazio com a ação de criar', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/cotacoes': () => adiada.promessa,
      'GET /api/produtos': () => respostaJson([]),
    });
    renderizarApp('/cotacoes');

    expect(await screen.findByText('Carregando cotações…')).toBeInTheDocument();
    adiada.responder(respostaJson([]));

    expect(await screen.findByText('Nenhuma cotação registrada')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Nova cotação' })).toHaveLength(2);
  });

  it('mostra o erro da API e permite tentar novamente', async () => {
    let falhar = true;
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/cotacoes': () =>
        falhar
          ? respostaProblema(500, 'Ocorreu um erro interno.')
          : respostaJson([COTACAO_RESUMO_TESTE]),
      'GET /api/produtos': () => respostaJson([]),
    });
    renderizarApp('/cotacoes');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar as cotações');
    falhar = false;
    await userEvent.setup().click(within(alerta).getByRole('button', { name: 'Tentar novamente' }));

    expect(await screen.findByRole('table', { name: 'Cotações registradas' })).toBeInTheDocument();
  });

  it('filtra por produto usando o parâmetro do backend', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/cotacoes': () => respostaJson([COTACAO_RESUMO_TESTE]),
      'GET /api/cotacoes?produtoId=20': () => respostaJson([]),
      'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
    });
    renderizarApp('/cotacoes');
    const usuario = userEvent.setup();
    await screen.findByRole('table', { name: 'Cotações registradas' });

    await selecionar(usuario, document.body, /Filtrar por produto/, 'Produto Ficticio A');

    expect(await screen.findByText('Nenhuma cotação para este produto')).toBeInTheDocument();
  });
});

async function preencherCotacao(usuario: UserEvent) {
  const formulario = await screen.findByRole('form', { name: 'Nova cotação' });
  await waitFor(() => {
    expect(within(formulario).getByLabelText(/^Produto/, { selector: 'input' })).toHaveAttribute(
      'placeholder',
      'Selecione',
    );
  });
  await selecionar(usuario, formulario, /^Produto/, 'Produto Ficticio A');
  await usuario.type(within(formulario).getByLabelText(/^Quantidade/), '2');
  await usuario.type(within(formulario).getByLabelText(/^Descrição/), 'Compra de teste');
  const opcao1 = screen.getByRole('region', { name: 'Opção 1' });
  await waitFor(() => {
    expect(within(opcao1).getByLabelText(/^Fornecedor/, { selector: 'input' })).toHaveAttribute(
      'placeholder',
      'Selecione',
    );
  });
  await selecionar(usuario, opcao1, /^Fornecedor/, 'Fornecedor Ficticio Ltda (11.222.333/0001-81)');
  await usuario.type(within(opcao1).getByLabelText(/^Condição de pagamento/), '30 dias');
  await usuario.type(within(opcao1).getByLabelText(/^Valor dos produtos/), '325,00');
  await usuario.type(within(opcao1).getByLabelText(/^Frete/), '0');
  await usuario.type(within(opcao1).getByLabelText(/^Alíquota de IPI/), '15');
  return formulario;
}

function rotasCriacao(perfil: object = USUARIO_USER_TESTE, extra = {}) {
  return {
    'GET /api/auth/me': () => respostaJson(perfil),
    'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
    'GET /api/fornecedores': () => respostaJson([FORNECEDOR_TESTE, OUTRO_FORNECEDOR]),
    'GET /api/comparacoes/7': () => respostaJson(COMPARACAO_TESTE),
    'GET /api/cotacoes/3/comparacoes': () => respostaJson([COMPARACAO_RESUMO_TESTE]),
    ...extra,
  };
}

describe('NovaCotacaoPage — criação', () => {
  it('envia a cotação no formato do DTO e abre a comparação inicial do backend', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasCriacao(USUARIO_USER_TESTE, { 'POST /api/cotacoes': () => adiada.promessa }),
    );
    renderizarApp('/cotacoes/nova');
    const usuario = userEvent.setup();
    const formulario = await preencherCotacao(usuario);

    await usuario.click(
      within(formulario).getByRole('button', { name: 'Criar cotação e comparar' }),
    );

    await waitFor(() => {
      expect(chamadas(fetchFalso, 'POST')).toHaveLength(1);
    });
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toEqual({
      produtoId: 20,
      quantidade: 2,
      descricao: 'Compra de teste',
      opcoes: [
        {
          fornecedorId: 10,
          nfeItemId: null,
          condicaoPagamento: '30 dias',
          observacao: null,
          valores: {
            valorProduto: 325,
            valorIpi: null,
            valorFrete: 0,
            valorSeguro: null,
            valorOutrasDespesas: null,
            valorDesconto: null,
          },
          dadosFiscais: {
            origemMercadoria: null,
            cfop: null,
            aliquotaIcms: null,
            aliquotaIpi: 15,
            aliquotaPis: null,
            aliquotaCofins: null,
          },
        },
      ],
    });

    adiada.responder(respostaJson(COTACAO_TESTE, 201));

    expect(await screen.findByRole('heading', { name: 'Comparação nº 7' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Cotação nº 3 criada. Resultado da comparação inicial nº 7.',
    );
    expect(screen.getByRole('table', { name: 'Alternativas classificadas' })).toBeInTheDocument();
  });

  it('permite incluir e remover opções antes de enviar', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasCriacao(USUARIO_USER_TESTE, {
        'POST /api/cotacoes': () => respostaJson(COTACAO_TESTE, 201),
      }),
    );
    renderizarApp('/cotacoes/nova');
    const usuario = userEvent.setup();
    const formulario = await preencherCotacao(usuario);

    await usuario.click(within(formulario).getByRole('button', { name: 'Adicionar outra opção' }));
    await usuario.click(within(formulario).getByRole('button', { name: 'Adicionar outra opção' }));
    const opcao2 = screen.getByRole('region', { name: 'Opção 2' });
    await selecionar(
      usuario,
      opcao2,
      /^Fornecedor/,
      'Outro Fornecedor Ficticio (11.222.333/0001-81)',
    );
    await usuario.click(within(formulario).getByRole('button', { name: 'Remover opção 3' }));
    expect(screen.queryByRole('region', { name: 'Opção 3' })).not.toBeInTheDocument();

    await usuario.click(
      within(formulario).getByRole('button', { name: 'Criar cotação e comparar' }),
    );

    await waitFor(() => {
      expect(chamadas(fetchFalso, 'POST')).toHaveLength(1);
    });
    const corpo = corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {}) as {
      opcoes: { fornecedorId: number; valores: unknown }[];
    };
    expect(corpo.opcoes.map((opcao) => opcao.fornecedorId)).toEqual([10, 11]);
    expect(corpo.opcoes[1]?.valores).toBeNull();
  });

  it('valida os campos antes de enviar, sem chamar a API', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(rotasCriacao());
    renderizarApp('/cotacoes/nova');
    const usuario = userEvent.setup();
    const formulario = await screen.findByRole('form', { name: 'Nova cotação' });
    const opcao1 = screen.getByRole('region', { name: 'Opção 1' });
    await usuario.type(within(formulario).getByLabelText(/^Quantidade/), '0');
    await usuario.type(within(opcao1).getByLabelText(/^Valor dos produtos/), '-5');
    await usuario.type(within(opcao1).getByLabelText(/^CFOP/), '61');

    await usuario.click(
      within(formulario).getByRole('button', { name: 'Criar cotação e comparar' }),
    );

    expect(await within(formulario).findByText('O produto é obrigatório.')).toBeInTheDocument();
    expect(
      within(formulario).getByText('A quantidade deve ser maior que zero.'),
    ).toBeInTheDocument();
    expect(
      within(opcao1).getByText('Informe o fornecedor ou o item de NF-e da opção.'),
    ).toBeInTheDocument();
    expect(within(opcao1).getByText('O valor não pode ser negativo.')).toBeInTheDocument();
    expect(within(opcao1).getByText('O CFOP deve ter 4 dígitos.')).toBeInTheDocument();
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);
  });

  it('mostra os erros de validação do backend nos campos de cada opção', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasCriacao(USUARIO_USER_TESTE, {
        'POST /api/cotacoes': () =>
          respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
            erros: {
              'opcoes[0].fornecedorId': 'Fornecedor não encontrado ou desativado.',
              opcoes: 'Informe no máximo 50 opções.',
            },
          }),
      }),
    );
    renderizarApp('/cotacoes/nova');
    const usuario = userEvent.setup();
    const formulario = await preencherCotacao(usuario);

    await usuario.click(
      within(formulario).getByRole('button', { name: 'Criar cotação e comparar' }),
    );

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('Não foi possível criar a cotação');
    expect(alerta).toHaveTextContent('Dados inválidos. Corrija os campos informados.');
    // Erro sem campo no formulário aparece na lista; o da opção, no próprio campo.
    expect(alerta).toHaveTextContent('Informe no máximo 50 opções.');
    expect(alerta).not.toHaveTextContent('Fornecedor não encontrado ou desativado.');
    const opcao1 = screen.getByRole('region', { name: 'Opção 1' });
    expect(
      within(opcao1).getByText('Fornecedor não encontrado ou desativado.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nova cotação' })).toBeInTheDocument();
  });

  it('USER informa o fornecedor; a escolha de item de NF-e aparece só para o ADMIN', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasCriacao());
    renderizarApp('/cotacoes/nova');
    const opcao1 = await screen.findByRole('region', { name: 'Opção 1' });

    expect(within(opcao1).getByLabelText(/^Fornecedor/, { selector: 'input' })).toBeInTheDocument();
    expect(within(opcao1).queryByLabelText(/^NF-e de referência/)).not.toBeInTheDocument();
  });

  it('ADMIN pode referenciar um item de NF-e importada', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasCriacao(USUARIO_ADMIN_TESTE, {
        'GET /api/nfe': () => respostaJson([NFE_RESUMO_TESTE]),
        'GET /api/nfe/1': () => respostaJson(NFE_TESTE),
        'POST /api/cotacoes': () => respostaJson(COTACAO_TESTE, 201),
      }),
    );
    renderizarApp('/cotacoes/nova');
    const usuario = userEvent.setup();
    const formulario = await screen.findByRole('form', { name: 'Nova cotação' });
    await waitFor(() => {
      expect(within(formulario).getByLabelText(/^Produto/, { selector: 'input' })).toHaveAttribute(
        'placeholder',
        'Selecione',
      );
    });
    await selecionar(usuario, formulario, /^Produto/, 'Produto Ficticio A');
    await usuario.type(within(formulario).getByLabelText(/^Quantidade/), '2');
    const opcao1 = screen.getByRole('region', { name: 'Opção 1' });
    await waitFor(() => {
      expect(
        within(opcao1).getByLabelText(/^NF-e de referência/, { selector: 'input' }),
      ).toHaveAttribute('placeholder', 'Nenhuma');
    });
    await selecionar(
      usuario,
      opcao1,
      /^NF-e de referência/,
      'Nº 123/1 — Fornecedor Ficticio Ltda (10/09/2026, 10:30)',
    );
    await waitFor(() => {
      expect(within(opcao1).getByLabelText(/^Item da NF-e/, { selector: 'input' })).toHaveAttribute(
        'placeholder',
        'Selecione',
      );
    });
    await selecionar(usuario, opcao1, /^Item da NF-e/, '1. Item ficticio A — Produto Ficticio A');

    await usuario.click(
      within(formulario).getByRole('button', { name: 'Criar cotação e comparar' }),
    );

    await waitFor(() => {
      expect(chamadas(fetchFalso, 'POST')).toHaveLength(1);
    });
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toMatchObject({
      opcoes: [{ fornecedorId: null, nfeItemId: 101, valores: null, dadosFiscais: null }],
    });
  });
});
