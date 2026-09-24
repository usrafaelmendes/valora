import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { FORNECEDOR_TESTE, PRODUTO_TESTE } from '../../test/dadosFicticios';
import {
  corpoEnviado,
  instalarFetchFalso,
  respostaAdiada,
  respostaJson,
  respostaProblema,
  USUARIO_ADMIN_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { selecionar } from '../../test/selecionar';
import { salvarSessaoTeste } from '../../test/sessao';
import type { RegraSelecionada, RegrasAplicaveisResponse } from '../../types/regraTributaria';

/** Configuração fictícia, só para testar a apresentação do resultado do backend. */
const REGRA_A: RegraSelecionada = {
  id: 1,
  nome: 'Regra A de teste',
  versao: 2,
  tributo: 'ICMS',
  formaAliquota: 'PERCENTUAL_FIXO',
  aliquota: 4,
  fator: 1,
  prioridade: 0,
};
const REGRA_B: RegraSelecionada = { ...REGRA_A, id: 2, nome: 'Regra B de teste', versao: 0 };
const REGRA_COMBINADA: RegraSelecionada = {
  ...REGRA_A,
  id: 3,
  nome: 'Regra PIS/COFINS de teste',
  tributo: 'PIS_COFINS',
  formaAliquota: 'ALIQUOTA_DA_NFE',
  aliquota: null,
};

const RESPOSTA: RegrasAplicaveisResponse = {
  operacao: {
    fornecedorId: FORNECEDOR_TESTE.id,
    tipoFornecedor: 'ATACADISTA',
    produtoId: null,
    ufOrigem: 'MG',
    ufDestino: 'GO',
    origemMercadoria: '6',
    cfop: '6102',
  },
  tributos: [
    {
      tributo: 'ICMS',
      situacao: 'CONFLITO',
      mensagem:
        'Mais de uma regra ativa se aplica com a mesma prioridade. Ajuste as prioridades ou as condições das regras.',
      regras: [REGRA_A, REGRA_B],
    },
    {
      tributo: 'IPI',
      situacao: 'SEM_REGRA',
      mensagem:
        'Nenhuma regra ativa se aplica a esta operação. A situação precisa ser configurada ou validada.',
      regras: [],
    },
    { tributo: 'PIS', situacao: 'APLICAVEL', mensagem: null, regras: [REGRA_COMBINADA] },
    { tributo: 'COFINS', situacao: 'APLICAVEL', mensagem: null, regras: [REGRA_COMBINADA] },
  ],
};

function abrirPagina(post: () => Response | Promise<Response>) {
  salvarSessaoTeste();
  const fetchFalso = instalarFetchFalso({
    'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
    'GET /api/fornecedores': () => respostaJson([FORNECEDOR_TESTE]),
    'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
    'POST /api/regras-tributarias/aplicaveis': post,
  });
  renderizarApp('/regras-tributarias/aplicaveis');
  return fetchFalso;
}

describe('RegrasAplicaveisPage', () => {
  it('envia a operação informada e mostra aplicável, sem regra e conflito como o backend devolveu', async () => {
    const adiada = respostaAdiada();
    const fetchFalso = abrirPagina(() => adiada.promessa);
    const usuario = userEvent.setup();
    await screen.findByRole('heading', { name: 'Conferir regras aplicáveis' });

    await selecionar(
      usuario,
      document.body,
      /^Fornecedor/,
      'Fornecedor Ficticio Ltda (11.222.333/0001-81)',
    );
    await selecionar(usuario, document.body, /Tipo de fornecedor/, 'Atacadista/revendedor');
    await selecionar(usuario, document.body, /UF de origem/, 'MG');
    await selecionar(
      usuario,
      document.body,
      /Origem da mercadoria/,
      '6 — Estrangeira — importação direta, sem similar nacional (lista CAMEX)',
    );
    await usuario.type(screen.getByLabelText(/CFOP/), '6102');
    await usuario.click(screen.getByRole('button', { name: 'Conferir' }));

    expect(corpoEnviado(fetchFalso.mock.calls.at(-1)?.[1] ?? {})).toEqual({
      fornecedorId: FORNECEDOR_TESTE.id,
      tipoFornecedor: 'ATACADISTA',
      produtoId: null,
      ufOrigem: 'MG',
      // Sem UF de destino: o backend usa o parâmetro UF_DESTINO.
      ufDestino: null,
      origemMercadoria: '6',
      cfop: '6102',
    });
    adiada.responder(respostaJson(RESPOSTA));

    const conflito = await screen.findByRole('article', { name: 'ICMS: Conflito' });
    expect(conflito).toHaveTextContent('Mais de uma regra ativa se aplica com a mesma prioridade.');
    const empatadas = within(conflito).getByRole('table', { name: 'Regras em conflito para ICMS' });
    expect(within(empatadas).getAllByRole('row')).toHaveLength(3);
    expect(empatadas).toHaveTextContent('Regra A de teste');
    expect(empatadas).toHaveTextContent('Regra B de teste');

    const semRegra = screen.getByRole('article', { name: 'IPI: Sem regra' });
    expect(semRegra).toHaveTextContent('A situação precisa ser configurada ou validada.');
    expect(within(semRegra).queryByRole('table')).not.toBeInTheDocument();

    const pis = screen.getByRole('article', { name: 'PIS: Regra aplicável' });
    expect(pis).toHaveTextContent('Regra PIS/COFINS de teste');
    expect(pis).toHaveTextContent('PIS/COFINS (combinado)');
    expect(pis).toHaveTextContent('Alíquota da NF-e');
    expect(screen.getByRole('article', { name: 'COFINS: Regra aplicável' })).toBeInTheDocument();

    // Operação considerada, com a UF de destino resolvida pelo backend.
    const resultado = screen.getByRole('region', { name: 'Resultado' });
    expect(resultado).toHaveTextContent('UF de destinoGO');
    expect(resultado).toHaveTextContent('Fornecedor Ficticio Ltda');
  });

  it('valida o CFOP antes de enviar', async () => {
    const fetchFalso = abrirPagina(() => respostaJson(RESPOSTA));
    const usuario = userEvent.setup();
    await screen.findByRole('heading', { name: 'Conferir regras aplicáveis' });

    await usuario.type(screen.getByLabelText(/CFOP/), '61');
    await usuario.click(screen.getByRole('button', { name: 'Conferir' }));

    expect(screen.getByText('O CFOP deve ter 4 dígitos.')).toBeInTheDocument();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('mostra o erro da API, inclusive 403', async () => {
    abrirPagina(() => respostaProblema(403, 'Você não tem permissão para acessar este recurso.'));
    const usuario = userEvent.setup();
    await screen.findByRole('heading', { name: 'Conferir regras aplicáveis' });

    await usuario.click(screen.getByRole('button', { name: 'Conferir' }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('Não foi possível conferir as regras');
    expect(alerta).toHaveTextContent('Você não tem permissão para acessar este recurso.');
  });

  it('é acessível pela página de regras', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/fornecedores': () => respostaJson([]),
      'GET /api/produtos': () => respostaJson([]),
      'GET /api/regras-tributarias': () => respostaJson([]),
    });
    renderizarApp('/regras-tributarias');

    await userEvent
      .setup()
      .click(await screen.findByRole('link', { name: 'Conferir regras aplicáveis' }));

    expect(
      await screen.findByRole('heading', { name: 'Conferir regras aplicáveis' }),
    ).toBeInTheDocument();
  });
});
