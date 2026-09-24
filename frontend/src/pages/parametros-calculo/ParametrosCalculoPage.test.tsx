import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
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
import type { ParametroCalculo } from '../../types/parametroCalculo';

/** Mesmas chaves e valores aceitos do backend; valores atuais fictícios. */
function parametro(parcial: Partial<ParametroCalculo> & { chave: string }): ParametroCalculo {
  return {
    descricao: `Descrição de ${parcial.chave}.`,
    valoresAceitos: [],
    valor: null,
    definido: false,
    versao: 0,
    atualizadoEm: '2026-09-01T12:00:00Z',
    ...parcial,
  };
}

const PARAMETROS: ParametroCalculo[] = [
  parametro({
    chave: 'FONTE_UF_ORIGEM',
    valoresAceitos: ['EMITENTE_NFE', 'CADASTRO_FORNECEDOR'],
    valor: 'EMITENTE_NFE',
    definido: true,
    versao: 1,
  }),
  parametro({
    chave: 'ARREDONDAMENTO_CREDITOS',
    valoresAceitos: ['POR_CREDITO', 'SOMENTE_TOTAL'],
  }),
  parametro({ chave: 'CFOPS_PARTICIPANTES' }),
  parametro({
    chave: 'COMPOSICAO_VALOR_OPERACAO',
    valoresAceitos: ['VALOR_PRODUTO', 'IPI', 'FRETE', 'SEGURO', 'OUTRAS_DESPESAS', 'DESCONTO'],
  }),
];

function chamadasPut(fetchFalso: ReturnType<typeof instalarFetchFalso>) {
  return fetchFalso.mock.calls.filter(([, init]) => init?.method === 'PUT');
}

/** Backend simulado: grava o valor enviado (a normalização real é do backend). */
function backendDeParametros() {
  let parametros = [...PARAMETROS];
  const atualizar =
    (chave: string) =>
    ({ init }: { init: RequestInit }) => {
      const { valor } = corpoEnviado(init) as { valor: string | null };
      const atual = parametros.find((p) => p.chave === chave);
      if (!atual) {
        return respostaProblema(404, 'Parâmetro de cálculo não encontrado.');
      }
      const salvo = { ...atual, valor, definido: valor !== null, versao: atual.versao + 1 };
      parametros = parametros.map((p) => (p.chave === chave ? salvo : p));
      return respostaJson(salvo);
    };
  return instalarFetchFalso({
    'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
    'GET /api/parametros-calculo': () => respostaJson(parametros),
    'PUT /api/parametros-calculo/FONTE_UF_ORIGEM': atualizar('FONTE_UF_ORIGEM'),
    'PUT /api/parametros-calculo/ARREDONDAMENTO_CREDITOS': atualizar('ARREDONDAMENTO_CREDITOS'),
    'PUT /api/parametros-calculo/CFOPS_PARTICIPANTES': atualizar('CFOPS_PARTICIPANTES'),
    'PUT /api/parametros-calculo/COMPOSICAO_VALOR_OPERACAO': atualizar('COMPOSICAO_VALOR_OPERACAO'),
  });
}

async function abrirPagina() {
  salvarSessaoTeste();
  renderizarApp('/parametros-calculo');
  return screen.findByRole('table', { name: 'Parâmetros de cálculo' });
}

function linhaDe(tabela: HTMLElement, chave: string): HTMLElement {
  const linha = within(tabela).getByText(chave).closest('tr');
  if (!linha) {
    throw new Error(`Linha de ${chave} não encontrada.`);
  }
  return linha;
}

describe('ParametrosCalculoPage — consulta', () => {
  it('mostra carregamento, valores definidos e parâmetros não definidos', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/parametros-calculo': () => adiada.promessa,
    });
    renderizarApp('/parametros-calculo');
    expect(await screen.findByText('Carregando parâmetros…')).toBeInTheDocument();
    adiada.responder(respostaJson(PARAMETROS));

    const tabela = await screen.findByRole('table', { name: 'Parâmetros de cálculo' });
    const definido = linhaDe(tabela, 'FONTE_UF_ORIGEM');
    expect(definido).toHaveTextContent('Fonte da UF de origem');
    expect(definido).toHaveTextContent('Descrição de FONTE_UF_ORIGEM.');
    expect(definido).toHaveTextContent('UF do emitente da NF-e');
    expect(definido).toHaveTextContent('v1');
    expect(linhaDe(tabela, 'ARREDONDAMENTO_CREDITOS')).toHaveTextContent('Não definido');

    const alerta = screen.getByText('Há parâmetros não definidos').closest('[role="alert"]');
    expect(alerta).toHaveTextContent('3 parâmetros ainda não foram definidos');
    expect(alerta).toHaveTextContent('Momento do arredondamento dos créditos');
    expect(alerta).toHaveTextContent('o cálculo fica incompleto');
  });

  it('mostra o erro da API e permite tentar novamente', async () => {
    let falhar = true;
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/parametros-calculo': () =>
        falhar ? respostaProblema(500, 'Ocorreu um erro interno.') : respostaJson(PARAMETROS),
    });
    renderizarApp('/parametros-calculo');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar os parâmetros de cálculo');
    falhar = false;
    await userEvent.setup().click(within(alerta).getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByRole('table', { name: 'Parâmetros de cálculo' })).toBeInTheDocument();
  });

  it('mostra a mensagem de acesso negado (403)', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/parametros-calculo': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    renderizarApp('/parametros-calculo');

    expect(await screen.findByRole('alert', {}, { timeout: 3000 })).toHaveTextContent(
      'Você não tem permissão para acessar este recurso.',
    );
  });
});

describe('ParametrosCalculoPage — alteração', () => {
  it('define um parâmetro de valor único entre os valores aceitos', async () => {
    const fetchFalso = backendDeParametros();
    const tabela = await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(
      within(tabela).getByRole('button', {
        name: 'Alterar Momento do arredondamento dos créditos',
      }),
    );
    const modal = await screen.findByRole('dialog', {
      name: 'Alterar: Momento do arredondamento dos créditos',
    });
    await selecionar(usuario, modal, /^Valor/, 'Arredondar somente o total (SOMENTE_TOTAL)');
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Parâmetro "Momento do arredondamento dos créditos" atualizado.',
    );
    expect(corpoEnviado(chamadasPut(fetchFalso)[0]?.[1] ?? {})).toEqual({ valor: 'SOMENTE_TOTAL' });
    await waitFor(() => {
      expect(linhaDe(tabela, 'ARREDONDAMENTO_CREDITOS')).toHaveTextContent(
        'Arredondar somente o total',
      );
    });
    expect(screen.getByText(/2 parâmetros ainda não foram definidos/)).toBeInTheDocument();
  });

  it('define uma lista de componentes e uma lista de CFOPs', async () => {
    const fetchFalso = backendDeParametros();
    const tabela = await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(
      within(tabela).getByRole('button', { name: 'Alterar Composição do valor da operação' }),
    );
    let modal = await screen.findByRole('dialog', {
      name: 'Alterar: Composição do valor da operação',
    });
    await selecionar(usuario, modal, /^Valores/, 'Valor dos produtos (VALOR_PRODUTO)');
    await selecionar(usuario, modal, /^Valores/, 'Desconto (subtraído) (DESCONTO)');
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar' }));
    await screen.findByRole('status');
    expect(corpoEnviado(chamadasPut(fetchFalso)[0]?.[1] ?? {})).toEqual({
      valor: 'VALOR_PRODUTO,DESCONTO',
    });

    await usuario.click(
      within(tabela).getByRole('button', { name: 'Alterar CFOPs participantes' }),
    );
    modal = await screen.findByRole('dialog', { name: 'Alterar: CFOPs participantes' });
    await usuario.type(within(modal).getByLabelText(/CFOPs/), '1102, 2102,');
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar' }));
    await waitFor(() => {
      expect(chamadasPut(fetchFalso)).toHaveLength(2);
    });
    expect(corpoEnviado(chamadasPut(fetchFalso)[1]?.[1] ?? {})).toEqual({ valor: '1102,2102' });
  });

  it('volta um parâmetro para "não definido" ao limpar o valor', async () => {
    const fetchFalso = backendDeParametros();
    const tabela = await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(
      within(tabela).getByRole('button', { name: 'Alterar Fonte da UF de origem' }),
    );
    const modal = await screen.findByRole('dialog', { name: 'Alterar: Fonte da UF de origem' });
    expect(within(modal).getByLabelText(/^Valor/, { selector: 'input' })).toHaveValue(
      'UF do emitente da NF-e (EMITENTE_NFE)',
    );
    await usuario.click(within(modal).getByRole('button', { name: 'Limpar valor (não definido)' }));
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Parâmetro "Fonte da UF de origem" voltou a "não definido".',
    );
    expect(corpoEnviado(chamadasPut(fetchFalso)[0]?.[1] ?? {})).toEqual({ valor: null });
    await waitFor(() => {
      expect(linhaDe(tabela, 'FONTE_UF_ORIGEM')).toHaveTextContent('Não definido');
    });
  });

  it('mostra no campo o erro de validação do backend e o 403', async () => {
    let tentativa = 0;
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/parametros-calculo': () => respostaJson(PARAMETROS),
      'PUT /api/parametros-calculo/CFOPS_PARTICIPANTES': () =>
        ++tentativa === 1
          ? respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
              erros: {
                valor: 'Informe CFOPs de 4 dígitos separados por vírgula (ex.: 1234,5678).',
              },
            })
          : respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    const tabela = await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(
      within(tabela).getByRole('button', { name: 'Alterar CFOPs participantes' }),
    );
    const modal = await screen.findByRole('dialog', { name: 'Alterar: CFOPs participantes' });
    const campo = within(modal).getByLabelText(/CFOPs/);
    await usuario.type(campo, '12,');
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar' }));

    await waitFor(() => {
      expect(campo).toHaveAccessibleDescription(/Informe CFOPs de 4 dígitos/);
    });

    await usuario.click(within(modal).getByRole('button', { name: 'Salvar' }));
    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'Você não tem permissão para acessar este recurso.',
    );
    expect(chamadasPut(fetchFalso)).toHaveLength(2);
  });
});
