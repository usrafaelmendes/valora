import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { Cotacao } from '../../types/cotacao';
import {
  COMPARACAO_RESUMO_TESTE,
  COMPARACAO_TESTE,
  COTACAO_TESTE,
  FORNECEDOR_TESTE,
} from '../../test/dadosFicticios';
import {
  corpoEnviado,
  instalarFetchFalso,
  respostaAdiada,
  respostaJson,
  respostaProblema,
  USUARIO_USER_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { selecionar } from '../../test/selecionar';
import { salvarSessaoTeste } from '../../test/sessao';

const OPCAO_INCLUIDA = {
  id: 401,
  fornecedorId: FORNECEDOR_TESTE.id,
  fornecedorRazaoSocial: FORNECEDOR_TESTE.razaoSocial,
  nfeItemId: null,
  condicaoPagamento: '90 dias',
  observacao: null,
  valores: null,
  dadosFiscais: null,
  criadoEm: '2026-09-21T10:00:00Z',
};

function rotasCotacao(cotacao: () => Cotacao = () => COTACAO_TESTE, extra = {}) {
  return {
    'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
    'GET /api/cotacoes/3': () => respostaJson(cotacao()),
    'GET /api/cotacoes/3/comparacoes': () => respostaJson([COMPARACAO_RESUMO_TESTE]),
    'GET /api/fornecedores': () => respostaJson([FORNECEDOR_TESTE]),
    ...extra,
  };
}

function linhas(tabela: HTMLElement) {
  return within(tabela).getAllByRole('row').slice(1);
}

describe('CotacaoDetalhePage — detalhes', () => {
  it('mostra a cotação, as opções com a situação na última comparação e o histórico', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasCotacao());
    renderizarApp('/cotacoes/3');

    expect(await screen.findByRole('heading', { name: 'Cotação nº 3' })).toBeInTheDocument();
    const dados = screen.getByRole('region', { name: 'Dados da cotação' });
    expect(dados).toHaveTextContent('Produto Ficticio A');
    expect(dados).toHaveTextContent('Cotação de teste');

    const ultima = screen.getByRole('region', { name: 'Última comparação' });
    expect(ultima).toHaveTextContent('Nº 7');
    expect(ultima).toHaveTextContent('3 de 9');
    expect(ultima).toHaveTextContent('Zeta Fornecedor Ficticio Ltda (R$ 249,11)');
    expect(within(ultima).getByRole('link', { name: 'Ver resultado completo' })).toHaveAttribute(
      'href',
      '/comparacoes/7',
    );

    const opcoes = screen.getByRole('table', { name: 'Opções da cotação' });
    const [primeira, , terceira, quarta] = linhas(opcoes);
    expect(primeira).toHaveTextContent('Zeta Fornecedor Ficticio Ltda');
    expect(primeira).toHaveTextContent('30 dias');
    expect(primeira).toHaveTextContent('Valor dos produtos: R$ 325,00');
    expect(primeira).toHaveTextContent('Frete: R$ 0,00');
    expect(primeira).toHaveTextContent('IPI: 15%');
    expect(primeira).toHaveTextContent('1º');
    expect(primeira).toHaveTextContent('Classificada');
    expect(terceira).toHaveTextContent('3º');
    expect(quarta).toHaveTextContent('Cálculo incompleto');
    expect(quarta).not.toHaveTextContent(/\dº/);

    const historico = await screen.findByRole('table', { name: 'Histórico de comparações' });
    expect(linhas(historico)[0]).toHaveTextContent('Nº 7');
    expect(linhas(historico)[0]).toHaveTextContent('Mais recente');
    expect(screen.queryByText('Nova comparação necessária')).not.toBeInTheDocument();
  });

  it('indica a opção fora da última comparação conforme o backend', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasCotacao(() => ({
        ...COTACAO_TESTE,
        opcoes: [...COTACAO_TESTE.opcoes, OPCAO_INCLUIDA],
        todasAsOpcoesComparadas: false,
      })),
    );
    renderizarApp('/cotacoes/3');

    expect(await screen.findByText('Nova comparação necessária')).toBeInTheDocument();
    const opcoes = screen.getByRole('table', { name: 'Opções da cotação' });
    expect(linhas(opcoes)[4]).toHaveTextContent('Não comparada');
  });

  it('mostra o carregamento e o erro da API (404)', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/cotacoes/3': () => adiada.promessa,
    });
    renderizarApp('/cotacoes/3');

    expect(await screen.findByText('Carregando cotação…')).toBeInTheDocument();
    adiada.responder(respostaProblema(404, 'Cotação não encontrada.'));

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar a cotação');
    expect(alerta).toHaveTextContent('Cotação não encontrada.');
  });

  it('trata endereço inválido sem chamar a API', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
    });
    renderizarApp('/cotacoes/abc');

    expect(
      await screen.findByRole('heading', { name: 'Cotação não encontrada' }),
    ).toBeInTheDocument();
    expect(fetchFalso).toHaveBeenCalledTimes(1);
  });
});

describe('CotacaoDetalhePage — adicionar opção', () => {
  it('inclui a opção, atualiza a cotação e informa que é preciso nova comparação', async () => {
    let cotacao: Cotacao = COTACAO_TESTE;
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasCotacao(() => cotacao, {
        'POST /api/cotacoes/3/opcoes': () => {
          cotacao = {
            ...COTACAO_TESTE,
            opcoes: [...COTACAO_TESTE.opcoes, OPCAO_INCLUIDA],
            todasAsOpcoesComparadas: false,
          };
          return respostaJson(OPCAO_INCLUIDA, 201);
        },
      }),
    );
    renderizarApp('/cotacoes/3');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Adicionar opção' }));
    const modal = await screen.findByRole('dialog', { name: 'Adicionar opção à cotação nº 3' });
    await waitFor(() => {
      expect(within(modal).getByLabelText(/^Fornecedor/, { selector: 'input' })).toHaveAttribute(
        'placeholder',
        'Selecione',
      );
    });
    await selecionar(
      usuario,
      modal,
      /^Fornecedor/,
      'Fornecedor Ficticio Ltda (11.222.333/0001-81)',
    );
    await usuario.type(within(modal).getByLabelText(/^Condição de pagamento/), '90 dias');
    await usuario.type(within(modal).getByLabelText(/^Valor dos produtos/), '200');
    await usuario.click(within(modal).getByRole('button', { name: 'Adicionar opção' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Opção nº 401 (Fornecedor Ficticio Ltda) incluída na cotação.',
    );
    const post = fetchFalso.mock.calls.find(([, init]) => init?.method === 'POST');
    expect(corpoEnviado(post?.[1] ?? {})).toEqual({
      fornecedorId: 10,
      nfeItemId: null,
      condicaoPagamento: '90 dias',
      observacao: null,
      valores: {
        valorProduto: 200,
        valorIpi: null,
        valorFrete: null,
        valorSeguro: null,
        valorOutrasDespesas: null,
        valorDesconto: null,
      },
      dadosFiscais: null,
    });

    // A cotação recarregada traz todasAsOpcoesComparadas = false.
    expect(await screen.findByText('Nova comparação necessária')).toBeInTheDocument();
    const opcoes = screen.getByRole('table', { name: 'Opções da cotação' });
    expect(linhas(opcoes)).toHaveLength(5);
    expect(linhas(opcoes)[4]).toHaveTextContent('Não comparada');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('valida a opção antes de enviar', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(rotasCotacao());
    renderizarApp('/cotacoes/3');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Adicionar opção' }));
    const modal = await screen.findByRole('dialog', { name: 'Adicionar opção à cotação nº 3' });
    await usuario.type(within(modal).getByLabelText(/^Alíquota de ICMS/), '150');
    await usuario.click(within(modal).getByRole('button', { name: 'Adicionar opção' }));

    expect(
      await within(modal).findByText('Informe o fornecedor ou o item de NF-e da opção.'),
    ).toBeInTheDocument();
    expect(within(modal).getByText('A alíquota deve estar entre 0 e 100.')).toBeInTheDocument();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('mostra o erro do backend (ex.: produto desativado) sem fechar o formulário', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasCotacao(undefined, {
        'POST /api/cotacoes/3/opcoes': () =>
          respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
            erros: {
              produtoId:
                'O produto da cotação está desativado: não é possível incluir novas opções.',
              'valores.valorProduto': 'Use no máximo 13 dígitos inteiros e 2 decimais.',
            },
          }),
      }),
    );
    renderizarApp('/cotacoes/3');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Adicionar opção' }));
    const modal = await screen.findByRole('dialog', { name: 'Adicionar opção à cotação nº 3' });
    await waitFor(() => {
      expect(within(modal).getByLabelText(/^Fornecedor/, { selector: 'input' })).toHaveAttribute(
        'placeholder',
        'Selecione',
      );
    });
    await selecionar(
      usuario,
      modal,
      /^Fornecedor/,
      'Fornecedor Ficticio Ltda (11.222.333/0001-81)',
    );
    await usuario.click(within(modal).getByRole('button', { name: 'Adicionar opção' }));

    const alerta = await within(modal).findByRole('alert');
    expect(alerta).toHaveTextContent(
      'O produto da cotação está desativado: não é possível incluir novas opções.',
    );
    expect(
      within(modal).getByText('Use no máximo 13 dígitos inteiros e 2 decimais.'),
    ).toBeInTheDocument();
    expect(alerta).not.toHaveTextContent('Use no máximo 13 dígitos');
  });
});

describe('CotacaoDetalhePage — nova comparação', () => {
  it('executa após confirmação e abre a nova comparação', async () => {
    const nova = { ...COMPARACAO_TESTE, id: 8 };
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasCotacao(undefined, {
        'POST /api/cotacoes/3/comparacoes': () => respostaJson(nova, 201),
        'GET /api/comparacoes/8': () => respostaJson(nova),
      }),
    );
    renderizarApp('/cotacoes/3');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Nova comparação' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Executar nova comparação' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Executar comparação' }));

    expect(await screen.findByRole('heading', { name: 'Comparação nº 8' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Nova comparação nº 8 executada');
    expect(
      fetchFalso.mock.calls.filter(([, init]) => init?.method === 'POST').map(([url]) => url),
    ).toEqual(['/api/cotacoes/3/comparacoes']);
  });
});
