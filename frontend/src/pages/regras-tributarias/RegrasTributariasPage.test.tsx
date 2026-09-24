import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { FORNECEDOR_TESTE, PRODUTO_TESTE } from '../../test/dadosFicticios';
import {
  corpoEnviado,
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  USUARIO_ADMIN_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { selecionar } from '../../test/selecionar';
import { salvarSessaoTeste } from '../../test/sessao';
import type { RegraTributaria, RegraTributariaRequest } from '../../types/regraTributaria';

/** Regras fictícias de configuração, só para os testes da interface. */
const REGRA_ATIVA: RegraTributaria = {
  id: 1,
  nome: 'Regra ICMS de teste',
  observacao: 'Observação de teste',
  tributo: 'ICMS',
  formaAliquota: 'PERCENTUAL_FIXO',
  aliquota: 7.5,
  fator: 1,
  prioridade: 5,
  ativa: true,
  tipoFornecedor: 'ATACADISTA',
  fornecedorId: FORNECEDOR_TESTE.id,
  produtoId: 999,
  ufOrigem: 'MG',
  ufDestino: 'GO',
  abrangenciaUf: 'INTERESTADUAL',
  origensMercadoria: ['1', '6'],
  cfops: ['6102'],
  versao: 3,
  atualizadoEm: '2026-09-01T15:00:00Z',
};

const REGRA_INATIVA: RegraTributaria = {
  ...REGRA_ATIVA,
  id: 2,
  nome: 'Regra IPI de teste',
  observacao: null,
  tributo: 'IPI',
  formaAliquota: 'ALIQUOTA_DA_NFE',
  aliquota: null,
  fator: 0.25,
  prioridade: 0,
  ativa: false,
  tipoFornecedor: null,
  fornecedorId: null,
  produtoId: null,
  ufOrigem: null,
  ufDestino: null,
  abrangenciaUf: null,
  origensMercadoria: [],
  cfops: [],
  versao: 0,
};

function chamadas(fetchFalso: ReturnType<typeof instalarFetchFalso>, metodo: string) {
  return fetchFalso.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === metodo);
}

/** Backend simulado com estado, incluindo o versionamento a cada alteração. */
function backendDeRegras(iniciais: RegraTributaria[] = [REGRA_ATIVA, REGRA_INATIVA]) {
  let regras = [...iniciais];
  return instalarFetchFalso({
    'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
    'GET /api/fornecedores': () => respostaJson([FORNECEDOR_TESTE]),
    'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
    'GET /api/regras-tributarias': () => respostaJson(regras),
    'GET /api/regras-tributarias?ativa=true': () => respostaJson(regras.filter((r) => r.ativa)),
    'GET /api/regras-tributarias?ativa=false&tributo=IPI': () =>
      respostaJson(regras.filter((r) => !r.ativa && r.tributo === 'IPI')),
    'POST /api/regras-tributarias': ({ init }) => {
      const dados = corpoEnviado(init) as RegraTributariaRequest;
      const criada: RegraTributaria = {
        ...REGRA_INATIVA,
        ...dados,
        id: 50,
        fator: dados.fator ?? 1,
        prioridade: dados.prioridade ?? 0,
        ativa: dados.ativa ?? true,
        versao: 0,
      };
      regras = [...regras, criada];
      return respostaJson(criada, 201);
    },
    'PUT /api/regras-tributarias/2': ({ init }) => {
      const dados = corpoEnviado(init) as RegraTributariaRequest;
      const atualizada: RegraTributaria = {
        ...REGRA_INATIVA,
        ...dados,
        fator: dados.fator ?? 1,
        prioridade: dados.prioridade ?? 0,
        ativa: dados.ativa ?? REGRA_INATIVA.ativa,
        versao: REGRA_INATIVA.versao + 1,
      };
      regras = regras.map((r) => (r.id === 2 ? atualizada : r));
      return respostaJson(atualizada);
    },
    'DELETE /api/regras-tributarias/1': () => {
      regras = regras.map((r) => (r.id === 1 ? { ...r, ativa: false, versao: r.versao + 1 } : r));
      return new Response(null, { status: 204 });
    },
  });
}

async function abrirPagina() {
  salvarSessaoTeste();
  renderizarApp('/regras-tributarias');
  const tabela = await screen.findByRole('table', { name: 'Regras tributárias' });
  // Aguarda os nomes de fornecedor e produto usados nas condições.
  await within(tabela).findByText(/Fornecedor: Fornecedor Ficticio Ltda/);
  return tabela;
}

describe('RegrasTributariasPage — listagem', () => {
  it('mostra tributo, origem da taxa, fator, prioridade, condições, situação e versão', async () => {
    backendDeRegras();
    const tabela = await abrirPagina();

    const [, ativa, inativa] = within(tabela).getAllByRole('row');
    if (!ativa || !inativa) {
      throw new Error('Linhas da tabela não encontradas.');
    }
    expect(ativa).toHaveTextContent('Regra ICMS de teste');
    expect(ativa).toHaveTextContent('Observação de teste');
    expect(ativa).toHaveTextContent('Percentual fixo: 7,5%');
    expect(ativa).toHaveTextContent('Tipo de fornecedor: Atacadista/revendedor');
    expect(ativa).toHaveTextContent('Fornecedor: Fornecedor Ficticio Ltda');
    // Produto desativado não aparece na lista de ativos: identificado pelo id.
    expect(ativa).toHaveTextContent('Produto: #999 (inativo)');
    expect(ativa).toHaveTextContent('UF de origem: MG');
    expect(ativa).toHaveTextContent('UF de destino: GO');
    expect(ativa).toHaveTextContent('Operação: Interestadual');
    expect(ativa).toHaveTextContent('Origem da mercadoria: 1, 6');
    expect(ativa).toHaveTextContent('CFOP: 6102');
    expect(ativa).toHaveTextContent('Ativa');
    expect(ativa).toHaveTextContent('v3');

    expect(inativa).toHaveTextContent('Alíquota da NF-e');
    expect(inativa).toHaveTextContent('0,25');
    expect(inativa).toHaveTextContent('Qualquer operação');
    expect(inativa).toHaveTextContent('Inativa');
    // Regra inativa pode ser editada (e reativada), mas não desativada de novo.
    expect(
      within(inativa).getByRole('button', { name: 'Editar Regra IPI de teste' }),
    ).toBeVisible();
    expect(within(inativa).queryByRole('button', { name: /Desativar/ })).not.toBeInTheDocument();
  });

  it('filtra por situação e tributo pelo backend', async () => {
    const fetchFalso = backendDeRegras();
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('radio', { name: 'Ativas' }));
    await waitFor(() => {
      expect(screen.getByRole('table', { name: 'Regras tributárias' })).not.toHaveTextContent(
        'Regra IPI de teste',
      );
    });

    await usuario.click(screen.getByRole('radio', { name: 'Inativas' }));
    await selecionar(usuario, document.body, /Filtrar por tributo/, 'IPI');
    await waitFor(() => {
      expect(screen.getByRole('table', { name: 'Regras tributárias' })).not.toHaveTextContent(
        'Regra ICMS de teste',
      );
    });
    expect(fetchFalso).toHaveBeenCalledWith(
      '/api/regras-tributarias?ativa=false&tributo=IPI',
      expect.anything(),
    );
  });

  it('mostra estado vazio sem regras', async () => {
    backendDeRegras([]);
    salvarSessaoTeste();
    renderizarApp('/regras-tributarias');

    expect(await screen.findByText('Nenhuma regra tributária cadastrada')).toBeInTheDocument();
  });

  it('mostra o erro da API e a mensagem de acesso negado (403)', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/fornecedores': () => respostaJson([]),
      'GET /api/produtos': () => respostaJson([]),
      'GET /api/regras-tributarias': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    renderizarApp('/regras-tributarias');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar as regras tributárias');
    expect(alerta).toHaveTextContent('Você não tem permissão para acessar este recurso.');
  });
});

describe('RegrasTributariasPage — cadastro e edição', () => {
  it('valida os campos antes de enviar', async () => {
    const fetchFalso = backendDeRegras();
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar regra' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar regra tributária' });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(within(modal).getByText('O nome é obrigatório.')).toBeInTheDocument();
    expect(within(modal).getByText('O tributo é obrigatório.')).toBeInTheDocument();
    expect(
      within(modal).getByText('A forma de obtenção da alíquota é obrigatória.'),
    ).toBeInTheDocument();

    await selecionar(usuario, modal, /Forma de obtenção/, 'Percentual fixo');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));
    expect(
      within(modal).getByText('A alíquota é obrigatória para PERCENTUAL_FIXO.'),
    ).toBeInTheDocument();
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);
  });

  it('cadastra a regra com as condições escolhidas', async () => {
    const fetchFalso = backendDeRegras();
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar regra' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar regra tributária' });
    await usuario.type(within(modal).getByLabelText(/^Nome/), 'Nova regra de teste');
    await selecionar(usuario, modal, /^Tributo/, 'PIS/COFINS (combinado)');
    await selecionar(usuario, modal, /Forma de obtenção/, 'Percentual fixo');
    await usuario.type(within(modal).getByLabelText(/Alíquota \(%\)/), '1,5');
    await usuario.type(within(modal).getByLabelText(/^Prioridade/), '10');
    await selecionar(usuario, modal, /Tipo de fornecedor/, 'Fabricante');
    await selecionar(
      usuario,
      modal,
      /^Fornecedor/,
      'Fornecedor Ficticio Ltda (11.222.333/0001-81)',
    );
    await selecionar(usuario, modal, /^Produto/, 'Produto Ficticio A');
    await selecionar(usuario, modal, /UF de origem/, 'AM');
    await selecionar(usuario, modal, /Operação/, 'Interestadual (origem diferente do destino)');
    await selecionar(
      usuario,
      modal,
      /Origem da mercadoria/,
      '2 — Estrangeira — adquirida no mercado interno, exceto a indicada no código 7',
    );
    await usuario.type(within(modal).getByLabelText(/CFOPs/), '6102,5102,');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Regra "Nova regra de teste" cadastrada.',
    );
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toEqual({
      nome: 'Nova regra de teste',
      observacao: null,
      tributo: 'PIS_COFINS',
      formaAliquota: 'PERCENTUAL_FIXO',
      aliquota: 1.5,
      fator: null,
      prioridade: 10,
      ativa: true,
      tipoFornecedor: 'FABRICANTE',
      fornecedorId: FORNECEDOR_TESTE.id,
      produtoId: PRODUTO_TESTE.id,
      ufOrigem: 'AM',
      ufDestino: null,
      abrangenciaUf: 'INTERESTADUAL',
      origensMercadoria: ['2'],
      cfops: ['6102', '5102'],
    });
    expect(screen.getByRole('table', { name: 'Regras tributárias' })).toHaveTextContent(
      'Nova regra de teste',
    );
  });

  it('mostra no campo o erro de coerência devolvido pela API e o conflito de nome', async () => {
    let tentativa = 0;
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/fornecedores': () => respostaJson([FORNECEDOR_TESTE]),
      'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
      'GET /api/regras-tributarias': () => respostaJson([REGRA_ATIVA]),
      'POST /api/regras-tributarias': () =>
        ++tentativa === 1
          ? respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
              erros: {
                abrangenciaUf: 'Abrangência INTERNA exige UF de origem igual à UF de destino.',
              },
            })
          : respostaProblema(409, 'Já existe uma regra tributária cadastrada com este nome.'),
    });
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar regra' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar regra tributária' });
    await usuario.type(within(modal).getByLabelText(/^Nome/), 'Regra ICMS de teste');
    await selecionar(usuario, modal, /^Tributo/, 'ICMS');
    await selecionar(usuario, modal, /Forma de obtenção/, 'Sem crédito');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    const abrangencia = within(modal).getByLabelText(/Operação/);
    await waitFor(() => {
      expect(abrangencia).toHaveAccessibleDescription(/Abrangência INTERNA exige/);
    });

    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));
    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'Já existe uma regra tributária cadastrada com este nome.',
    );
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(2);
  });

  it('edita a regra com os dados atuais, informa o versionamento e permite reativar', async () => {
    const fetchFalso = backendDeRegras();
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Editar Regra IPI de teste' }));
    const modal = await screen.findByRole('dialog', { name: 'Editar regra tributária' });
    expect(modal).toHaveTextContent('a regra passa da versão 0 para uma nova versão');
    expect(within(modal).getByLabelText(/^Nome/)).toHaveValue('Regra IPI de teste');
    expect(within(modal).getByLabelText(/^Fator/)).toHaveValue('0,25');
    // A alíquota só existe para percentual fixo.
    expect(within(modal).queryByLabelText(/Alíquota \(%\)/)).not.toBeInTheDocument();

    const ativa = within(modal).getByRole('switch', { name: /Regra ativa/ });
    expect(ativa).not.toBeChecked();
    await usuario.click(ativa);
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar alterações' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Regra "Regra IPI de teste" atualizada (versão 1).',
    );
    expect(corpoEnviado(chamadas(fetchFalso, 'PUT')[0]?.[1] ?? {})).toMatchObject({
      nome: 'Regra IPI de teste',
      tributo: 'IPI',
      formaAliquota: 'ALIQUOTA_DA_NFE',
      aliquota: null,
      fator: 0.25,
      prioridade: 0,
      ativa: true,
    });
    const linha = within(screen.getByRole('table', { name: 'Regras tributárias' }))
      .getByText('Regra IPI de teste')
      .closest('tr');
    expect(linha).toHaveTextContent('Ativa');
    expect(linha).toHaveTextContent('v1');
  });

  it('esconde o fator quando a forma é "sem crédito"', async () => {
    backendDeRegras();
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar regra' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar regra tributária' });
    expect(within(modal).getByLabelText(/^Fator/)).toBeInTheDocument();
    await selecionar(usuario, modal, /Forma de obtenção/, 'Sem crédito');
    expect(within(modal).queryByLabelText(/^Fator/)).not.toBeInTheDocument();
    expect(modal).toHaveTextContent('Declara crédito zero');
  });
});

describe('RegrasTributariasPage — desativação', () => {
  it('confirma, explica que o histórico é mantido e mostra a regra como inativa', async () => {
    const fetchFalso = backendDeRegras();
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Desativar Regra ICMS de teste' }));
    const modal = await screen.findByRole('dialog', { name: 'Desativar regra tributária' });
    expect(modal).toHaveTextContent('não é apagada');
    expect(modal).toHaveTextContent('mantêm o registro da regra e da versão usadas');

    await usuario.click(within(modal).getByRole('button', { name: 'Desativar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Regra "Regra ICMS de teste" desativada.',
    );
    expect(chamadas(fetchFalso, 'DELETE')).toHaveLength(1);
    const linha = within(screen.getByRole('table', { name: 'Regras tributárias' }))
      .getByText('Regra ICMS de teste')
      .closest('tr');
    await waitFor(() => {
      expect(linha).toHaveTextContent('Inativa');
    });
  });

  it('cancelar não desativa, e o erro da API aparece na confirmação', async () => {
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/fornecedores': () => respostaJson([FORNECEDOR_TESTE]),
      'GET /api/produtos': () => respostaJson([PRODUTO_TESTE]),
      'GET /api/regras-tributarias': () => respostaJson([REGRA_ATIVA]),
      'DELETE /api/regras-tributarias/1': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Desativar Regra ICMS de teste' }));
    let modal = await screen.findByRole('dialog', { name: 'Desativar regra tributária' });
    await usuario.click(within(modal).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(chamadas(fetchFalso, 'DELETE')).toHaveLength(0);

    await usuario.click(screen.getByRole('button', { name: 'Desativar Regra ICMS de teste' }));
    modal = await screen.findByRole('dialog', { name: 'Desativar regra tributária' });
    await usuario.click(within(modal).getByRole('button', { name: 'Desativar' }));
    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'Você não tem permissão para acessar este recurso.',
    );
  });
});
