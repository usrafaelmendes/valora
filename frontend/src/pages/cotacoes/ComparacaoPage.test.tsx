import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Comparacao } from '../../types/cotacao';
import {
  ALTERNATIVAS_TESTE,
  COMPARACAO_RESUMO_TESTE,
  COMPARACAO_TESTE,
  COTACAO_TESTE,
} from '../../test/dadosFicticios';
import {
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  USUARIO_USER_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { salvarSessaoTeste } from '../../test/sessao';

function rotasComparacao(comparacao: Comparacao = COMPARACAO_TESTE, extra = {}) {
  return {
    'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
    [`GET /api/comparacoes/${String(comparacao.id)}`]: () => respostaJson(comparacao),
    [`GET /api/cotacoes/${String(comparacao.cotacaoId)}/comparacoes`]: () =>
      respostaJson([COMPARACAO_RESUMO_TESTE]),
    ...extra,
  };
}

function linhas(tabela: HTMLElement) {
  return within(tabela).getAllByRole('row').slice(1);
}

describe('ComparacaoPage — alternativas classificadas', () => {
  it('mostra as classificadas na ordem do backend, com posição, créditos e custo efetivo', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');

    const tabela = await screen.findByRole('table', { name: 'Alternativas classificadas' });
    const [primeira, segunda, terceira] = linhas(tabela);
    expect(linhas(tabela)).toHaveLength(3);

    expect(primeira).toHaveTextContent('1º');
    expect(primeira).toHaveTextContent('Zeta Fornecedor Ficticio Ltda');
    expect(primeira).toHaveTextContent('Fabricante');
    expect(primeira).toHaveTextContent('30 dias');
    expect(primeira).toHaveTextContent('Prazo base do fornecedor: 28/56/84 dias');
    expect(primeira).toHaveTextContent('R$ 325,00');
    expect(primeira).toHaveTextContent('ICMS: R$ 22,75');
    expect(primeira).toHaveTextContent('IPI: R$ 32,50');
    expect(primeira).toHaveTextContent('PIS/COFINS (combinado): R$ 20,64');
    expect(primeira).toHaveTextContent('R$ 75,89');
    expect(primeira).toHaveTextContent('R$ 249,11');

    // Ordem do backend, e não alfabética: "Alfa…" vem depois de "Zeta…".
    expect(segunda).toHaveTextContent('2º');
    expect(segunda).toHaveTextContent('Alfa Distribuidora Ficticia SA');
    expect(segunda).toHaveTextContent('Atacadista/revendedor');
    expect(segunda).toHaveTextContent('Não informado');
    expect(terceira).toHaveTextContent('3º');
    expect(terceira).toHaveTextContent('Gama Pecas Ficticias ME');
    expect(terceira).toHaveTextContent('R$ 279,50');
  });

  it('não reordena nem recalcula: a tabela segue exatamente a lista recebida', async () => {
    const [primeira, segunda] = ALTERNATIVAS_TESTE;
    if (!primeira || !segunda) {
      throw new Error('Dados de teste incompletos.');
    }
    // Lista propositalmente fora da ordem de custo: o frontend não pode "corrigir" o ranking.
    const comparacao: Comparacao = {
      ...COMPARACAO_TESTE,
      alternativas: [
        { ...primeira, posicao: 1, empate: false, custoEfetivo: 999.99, totalCreditos: 1 },
        { ...segunda, posicao: 2, empate: false, custoEfetivo: 10 },
      ],
    };
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao(comparacao));
    renderizarApp('/comparacoes/7');

    const tabela = await screen.findByRole('table', { name: 'Alternativas classificadas' });
    const [linha1, linha2] = linhas(tabela);
    expect(linha1).toHaveTextContent('1º');
    expect(linha1).toHaveTextContent('Zeta Fornecedor Ficticio Ltda');
    // Valores exibidos como vieram, sem recalcular valor da operação − créditos.
    expect(linha1).toHaveTextContent('R$ 999,99');
    expect(linha1).toHaveTextContent('R$ 1,00');
    expect(linha2).toHaveTextContent('2º');
    expect(linha2).toHaveTextContent('R$ 10,00');
  });

  it('destaca a 1ª posição e indica o empate informado pelo backend', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');

    const destaque = await screen.findByRole('region', { name: 'Primeira posição' });
    expect(destaque).toHaveTextContent('1º lugar');
    expect(destaque).toHaveTextContent('Empate');
    expect(destaque).toHaveTextContent('Zeta Fornecedor Ficticio Ltda');
    expect(destaque).toHaveTextContent('R$ 249,11');
    expect(destaque).toHaveTextContent('mesmo custo efetivo de outra opção classificada');

    const tabela = screen.getByRole('table', { name: 'Alternativas classificadas' });
    const [primeira, segunda, terceira] = linhas(tabela);
    expect(primeira).toHaveTextContent('Empate');
    expect(segunda).toHaveTextContent('Empate');
    expect(terceira).not.toHaveTextContent('Empate');
    expect(screen.getByText(/^Ordem registrada pelo backend/)).toHaveTextContent(
      'Empate: ordem de cadastro da opção na cotação.',
    );
  });

  it('informa quando nenhuma opção foi classificada', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasComparacao({ ...COMPARACAO_TESTE, alternativas: [], totalClassificadas: 0 }),
    );
    renderizarApp('/comparacoes/7');

    expect(await screen.findByText('Nenhuma opção classificada')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Primeira posição' })).not.toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Opções não classificadas' })).toBeInTheDocument();
  });
});

describe('ComparacaoPage — não classificadas', () => {
  it('lista separadamente cada situação com o motivo do backend, sem posição', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');

    const tabela = await screen.findByRole('table', { name: 'Opções não classificadas' });
    const [incompleto, fornecedor, produto, cfop, cfopsNaoDefinidos, quantidade] = linhas(tabela);
    expect(linhas(tabela)).toHaveLength(6);

    expect(incompleto).toHaveTextContent('Cálculo incompleto');
    expect(incompleto).toHaveTextContent(
      'O custo efetivo não pôde ser calculado: Nenhuma regra ativa se aplica ao ICMS (teste).',
    );
    expect(incompleto).toHaveTextContent('Incompleto');
    // Custo não obtido nunca vira zero.
    expect(incompleto).toHaveTextContent('Não obtido');
    expect(incompleto).not.toHaveTextContent('R$ 0,00');

    expect(fornecedor).toHaveTextContent('Fornecedor desativado');
    expect(fornecedor).toHaveTextContent('Desativado');
    expect(fornecedor).toHaveTextContent('O fornecedor está desativado (teste).');
    expect(fornecedor).toHaveTextContent('Não calculada');

    expect(produto).toHaveTextContent('Produto desativado');
    expect(produto).toHaveTextContent('O produto está desativado (teste).');

    expect(cfop).toHaveTextContent('CFOP não participante');
    expect(cfop).toHaveTextContent('O CFOP 6910 da operação não está em CFOPS_PARTICIPANTES');

    expect(cfopsNaoDefinidos).toHaveTextContent('CFOPs participantes não definidos');
    expect(cfopsNaoDefinidos).toHaveTextContent('CFOPS_PARTICIPANTES não foi definido (teste).');

    expect(quantidade).toHaveTextContent('Quantidade divergente');
    expect(quantidade).toHaveTextContent(
      'A quantidade da operação calculada (5) difere da quantidade da cotação (2) (teste).',
    );
    // O custo calculado continua visível, mas fora do ranking.
    expect(quantidade).toHaveTextContent('R$ 400,00');

    for (const linha of linhas(tabela)) {
      expect(linha).not.toHaveTextContent(/\dº/);
    }
    const classificadas = screen.getByRole('table', { name: 'Alternativas classificadas' });
    expect(classificadas).not.toHaveTextContent('Fornecedor 306 Ficticio');
  });
});

describe('ComparacaoPage — rastreabilidade', () => {
  it('mostra créditos individuais, regras com versão, parâmetros e totais do cálculo', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');
    const usuario = userEvent.setup();

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Detalhes do cálculo: 1º lugar — Zeta Fornecedor Ficticio Ltda',
      }),
    );

    const creditos = await screen.findByRole('table', { name: 'Créditos do cálculo 501' });
    const pisCofins = linhas(creditos)[2];
    expect(pisCofins).toHaveTextContent('PIS/COFINS (combinado)');
    expect(pisCofins).toHaveTextContent('PIS/COFINS 6,35% (teste) (v3)');
    expect(pisCofins).toHaveTextContent('Percentual fixo');
    expect(pisCofins).toHaveTextContent('6,35%');
    expect(pisCofins).toHaveTextContent('R$ 20,6375');
    expect(pisCofins).toHaveTextContent('R$ 20,64');
    expect(linhas(creditos)[0]).toHaveTextContent('ICMS interestadual (teste) (v2)');

    const painel = creditos.closest('[role="region"]') ?? document.body;
    const detalhes = within(painel as HTMLElement);
    expect(detalhes.getByText('Fonte dos valores da operação').nextSibling).toHaveTextContent(
      'Valores informados no cálculo (ex.: cotação)',
    );
    expect(detalhes.getByText('Critério de arredondamento').nextSibling).toHaveTextContent(
      'Não definido',
    );
    expect(
      detalhes.getByText('Total de créditos sem arredondamento').nextSibling,
    ).toHaveTextContent('R$ 75,8875');
    expect(detalhes.getByText('Diferença de arredondamento').nextSibling).toHaveTextContent(
      'R$ 0,0025',
    );
    expect(detalhes.getByText('Base dos créditos').nextSibling).toHaveTextContent('R$ 325,00');
    expect(detalhes.getByText('Nenhuma pendência ou aviso registrado.')).toBeInTheDocument();
  });

  it('mostra as pendências do cálculo incompleto, bloqueantes e avisos', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');
    const usuario = userEvent.setup();

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Detalhes do cálculo: Não classificada — Fornecedor 301 Ficticio',
      }),
    );

    const pendencias = await screen.findByRole('table', { name: 'Pendências do cálculo 504' });
    const [bloqueante, aviso] = linhas(pendencias);
    expect(bloqueante).toHaveTextContent('Regra ausente');
    expect(bloqueante).toHaveTextContent('Bloqueante');
    expect(bloqueante).toHaveTextContent('Nenhuma regra ativa se aplica ao ICMS (teste).');
    expect(aviso).toHaveTextContent('Componente ausente na NF-e');
    expect(aviso).toHaveTextContent('Aviso');

    const creditos = screen.getByRole('table', { name: 'Créditos do cálculo 504' });
    expect(linhas(creditos)[0]).toHaveTextContent('Regra ausente');
    expect(linhas(creditos)[0]).toHaveTextContent('Nenhuma regra ativa se aplica ao tributo.');
  });

  it('explica quando a opção não foi calculada', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');
    const usuario = userEvent.setup();

    const controle = await screen.findByRole('button', {
      name: 'Detalhes do cálculo: Não classificada — Fornecedor 302 Ficticio',
    });
    await usuario.click(controle);

    const painel = document.getElementById(controle.getAttribute('aria-controls') ?? '');
    expect(painel).toHaveTextContent(
      'A opção não foi calculada nesta comparação. Motivo registrado: O fornecedor está desativado (teste).',
    );
  });

  it('mostra a configuração gravada: parâmetros (inclusive não definidos) e regras com versão', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComparacao());
    renderizarApp('/comparacoes/7');

    const titulo = await screen.findByRole('heading', { name: 'Configuração utilizada' });
    const secao = titulo.closest('section');
    if (!secao) {
      throw new Error('Seção de configuração não encontrada.');
    }
    expect(within(secao).getByText('UF de destino (UF_DESTINO)').nextSibling).toHaveTextContent(
      'GO',
    );
    expect(
      within(secao).getByText('CFOPs participantes (CFOPS_PARTICIPANTES)').nextSibling,
    ).toHaveTextContent('Não definido');

    const regras = within(secao).getByRole('table', {
      name: 'Regras tributárias ativas na comparação',
    });
    expect(linhas(regras)[0]).toHaveTextContent('PIS/COFINS 6,35% (teste)');
    expect(linhas(regras)[0]).toHaveTextContent('v3');
    expect(linhas(regras)[1]).toHaveTextContent('ICMS interestadual (teste)');
    expect(linhas(regras)[1]).toHaveTextContent('v2');
    expect(linhas(regras)[1]).toHaveTextContent('5');
  });

  it('mostra o resumo e avisa quando existe comparação mais recente', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasComparacao(COMPARACAO_TESTE, {
        'GET /api/cotacoes/3/comparacoes': () =>
          respostaJson([{ ...COMPARACAO_RESUMO_TESTE, id: 8 }, COMPARACAO_RESUMO_TESTE]),
      }),
    );
    renderizarApp('/comparacoes/7');

    const resumo = await screen.findByRole('region', { name: 'Resumo da comparação' });
    expect(resumo).toHaveTextContent('Produto Ficticio A');
    expect(resumo).toHaveTextContent('20/09/2026, 12:00');
    expect(within(resumo).getByText('Classificadas').nextSibling).toHaveTextContent('3');
    expect(within(resumo).getByText('Não classificadas').nextSibling).toHaveTextContent('6');

    const alerta = await screen.findByText(/Existe uma comparação mais recente/);
    expect(within(alerta).getByRole('link', { name: 'comparação nº 8' })).toHaveAttribute(
      'href',
      '/comparacoes/8',
    );
    expect(screen.getByRole('link', { name: 'Ver cotação' })).toHaveAttribute(
      'href',
      '/cotacoes/3',
    );
  });

  it('mostra o erro da API (404) e permite tentar novamente', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/comparacoes/99': () => respostaProblema(404, 'Comparação não encontrada.'),
    });
    renderizarApp('/comparacoes/99');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar a comparação');
    expect(alerta).toHaveTextContent('Comparação não encontrada.');
    expect(within(alerta).getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument();
  });
});

describe('ComparacaoPage — download', () => {
  /** Nome de cada arquivo oferecido ao navegador (atributo download do link clicado). */
  let baixados: string[];
  const criarUrl = vi.fn<(objeto: Blob) => string>(() => 'blob:comparacao');
  const revogarUrl = vi.fn<(url: string) => void>();

  beforeEach(() => {
    baixados = [];
    criarUrl.mockClear();
    revogarUrl.mockClear();
    // URLs de objeto não existem no jsdom.
    URL.createObjectURL = criarUrl;
    URL.revokeObjectURL = revogarUrl;
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      baixados.push(this.download);
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('baixa o CSV do backend com o nome informado, sem recalcular nem gerar o arquivo', async () => {
    const conteudo = '\uFEFFPosição;Fornecedor\r\n1;Zeta\r\n';
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasComparacao(COMPARACAO_TESTE, {
        'GET /api/comparacoes/7/download': () =>
          new Response(conteudo, {
            status: 200,
            headers: {
              'Content-Type': 'text/csv;charset=UTF-8',
              'Content-Disposition': 'attachment; filename="comparacao-7-cotacao-3.csv"',
            },
          }),
      }),
    );
    renderizarApp('/comparacoes/7');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Baixar tabela (CSV)' }));

    await waitFor(() => {
      expect(baixados).toEqual(['comparacao-7-cotacao-3.csv']);
    });
    const blob = criarUrl.mock.calls[0]?.[0];
    if (!blob) {
      throw new Error('Nenhum arquivo foi oferecido ao navegador.');
    }
    // Bytes idênticos aos do backend (inclusive o BOM do UTF-8): o arquivo não é reescrito.
    expect(Array.from(new Uint8Array(await blob.arrayBuffer()))).toEqual(
      Array.from(new TextEncoder().encode(conteudo)),
    );
    expect(revogarUrl).toHaveBeenCalledWith('blob:comparacao');

    const chamadas = fetchFalso.mock.calls.map(
      ([url, init]) => `${init?.method ?? 'GET'} ${typeof url === 'string' ? url : ''}`,
    );
    expect(chamadas).toContain('GET /api/comparacoes/7/download');
    // Nenhuma nova comparação ou cálculo é disparado pelo download.
    expect(chamadas.filter((chamada) => chamada.startsWith('POST'))).toHaveLength(0);
  });

  it('usa um nome padrão quando o backend não informa o arquivo', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasComparacao(COMPARACAO_TESTE, {
        'GET /api/comparacoes/7/download': () =>
          new Response('a;b', { status: 200, headers: { 'Content-Type': 'text/csv' } }),
      }),
    );
    renderizarApp('/comparacoes/7');

    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: 'Baixar tabela (CSV)' }));

    await waitFor(() => {
      expect(baixados).toEqual(['comparacao-7.csv']);
    });
  });

  it('mostra o erro quando o download falha', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(
      rotasComparacao(COMPARACAO_TESTE, {
        'GET /api/comparacoes/7/download': () =>
          respostaProblema(404, 'Comparação não encontrada.'),
      }),
    );
    renderizarApp('/comparacoes/7');

    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: 'Baixar tabela (CSV)' }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('Não foi possível baixar a tabela');
    expect(alerta).toHaveTextContent('Comparação não encontrada.');
    expect(baixados).toEqual([]);
    // O resultado continua visível.
    expect(screen.getByRole('table', { name: 'Alternativas classificadas' })).toBeInTheDocument();
  });
});

describe('ComparacaoPage — nova comparação', () => {
  it('confirma, cria uma nova comparação e abre o novo resultado', async () => {
    const nova: Comparacao = { ...COMPARACAO_TESTE, id: 8 };
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasComparacao(COMPARACAO_TESTE, {
        'POST /api/cotacoes/3/comparacoes': () => respostaJson(nova, 201),
        'GET /api/comparacoes/8': () => respostaJson(nova),
      }),
    );
    renderizarApp('/comparacoes/7');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Nova comparação' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Executar nova comparação' });
    expect(dialogo).toHaveTextContent('Uma nova comparação da cotação nº 3 será criada');
    expect(dialogo).toHaveTextContent('As comparações anteriores continuam no histórico');
    // Nada é enviado antes da confirmação.
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);

    await usuario.click(within(dialogo).getByRole('button', { name: 'Executar comparação' }));

    expect(await screen.findByRole('heading', { name: 'Comparação nº 8' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(
      'Nova comparação nº 8 executada com a configuração vigente.',
    );
    expect(fetchFalso.mock.calls.filter(([, init]) => init?.method === 'POST')).toHaveLength(1);
    expect(COTACAO_TESTE.ultimaComparacao?.id).toBe(7);
  });

  it('não executa nada quando o usuário cancela e mostra o erro da API', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso(
      rotasComparacao(COMPARACAO_TESTE, {
        'POST /api/cotacoes/3/comparacoes': () => respostaProblema(404, 'Cotação não encontrada.'),
      }),
    );
    renderizarApp('/comparacoes/7');
    const usuario = userEvent.setup();

    await usuario.click(await screen.findByRole('button', { name: 'Nova comparação' }));
    let dialogo = await screen.findByRole('dialog', { name: 'Executar nova comparação' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);

    await usuario.click(screen.getByRole('button', { name: 'Nova comparação' }));
    dialogo = await screen.findByRole('dialog', { name: 'Executar nova comparação' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Executar comparação' }));

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Cotação não encontrada.');
    expect(screen.getByRole('heading', { name: 'Comparação nº 7' })).toBeInTheDocument();
  });
});
