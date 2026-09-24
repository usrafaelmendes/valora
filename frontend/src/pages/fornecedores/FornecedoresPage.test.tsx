import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import {
  corpoEnviado,
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  USUARIO_ADMIN_TESTE,
  USUARIO_USER_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { salvarSessaoTeste } from '../../test/sessao';
import type { Fornecedor, FornecedorRequest } from '../../types/fornecedor';

/** Dados fictícios, exclusivos para testes (CNPJs de exemplo, sem relação com empresas reais). */
const FABRICANTE: Fornecedor = {
  id: 1,
  razaoSocial: 'Fabricante Teste Ltda',
  cnpj: '11222333000181',
  uf: 'GO',
  tipo: 'FABRICANTE',
  prazoPagamentoBase: '28/56/84 dias',
  ativo: true,
};
const ATACADISTA: Fornecedor = {
  id: 2,
  razaoSocial: 'Atacadista Teste S.A.',
  cnpj: '12ABC34501DE35',
  uf: 'MG',
  tipo: 'ATACADISTA',
  prazoPagamentoBase: null,
  ativo: true,
};

type Usuario = typeof USUARIO_ADMIN_TESTE | typeof USUARIO_USER_TESTE;

/** Backend simulado com estado: a listagem reflete cadastros, edições e desativações. */
function backendDeFornecedores(
  usuario: Usuario,
  iniciais: Fornecedor[] = [FABRICANTE, ATACADISTA],
) {
  let fornecedores = [...iniciais];
  return instalarFetchFalso({
    'GET /api/auth/me': () => respostaJson(usuario),
    'GET /api/fornecedores': () => respostaJson(fornecedores),
    'POST /api/fornecedores': ({ init }) => {
      const criado: Fornecedor = {
        id: 100,
        ...(corpoEnviado(init) as FornecedorRequest),
        ativo: true,
      };
      fornecedores = [...fornecedores, criado];
      return respostaJson(criado, 201);
    },
    'PUT /api/fornecedores/2': ({ init }) => {
      const atualizado: Fornecedor = {
        ...ATACADISTA,
        ...(corpoEnviado(init) as FornecedorRequest),
      };
      fornecedores = fornecedores.map((f) => (f.id === 2 ? atualizado : f));
      return respostaJson(atualizado);
    },
    'DELETE /api/fornecedores/1': () => {
      fornecedores = fornecedores.filter((f) => f.id !== 1);
      return new Response(null, { status: 204 });
    },
  });
}

function chamadas(fetchFalso: ReturnType<typeof instalarFetchFalso>, metodo: string) {
  return fetchFalso.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === metodo);
}

async function abrirPagina() {
  salvarSessaoTeste();
  renderizarApp('/fornecedores');
  return screen.findByRole('table', { name: 'Fornecedores' });
}

/** Escolhe uma opção de Select; no jsdom a lista abre em transição, ainda marcada como oculta. */
async function selecionar(usuario: UserEvent, modal: HTMLElement, campo: RegExp, opcao: string) {
  await usuario.click(within(modal).getByLabelText(campo));
  await usuario.click(await screen.findByRole('option', { name: opcao, hidden: true }));
}

describe('FornecedoresPage — consulta', () => {
  it('USER consulta a listagem formatada, sem ações de escrita', async () => {
    backendDeFornecedores(USUARIO_USER_TESTE);

    const tabela = await abrirPagina();

    const linhas = within(tabela).getAllByRole('row');
    expect(linhas).toHaveLength(3);
    expect(linhas[1]).toHaveTextContent('Fabricante Teste Ltda');
    expect(linhas[1]).toHaveTextContent('11.222.333/0001-81');
    expect(linhas[1]).toHaveTextContent('GO');
    expect(linhas[1]).toHaveTextContent('Fabricante');
    expect(linhas[1]).toHaveTextContent('28/56/84 dias');
    expect(linhas[2]).toHaveTextContent('12.ABC.345/01DE-35');
    expect(linhas[2]).toHaveTextContent('Atacadista/revendedor');
    expect(linhas[2]).toHaveTextContent('Não informado');
    expect(screen.queryByRole('button', { name: /cadastrar|editar|desativar/i })).toBeNull();
  });

  it('mostra o estado vazio', async () => {
    backendDeFornecedores(USUARIO_ADMIN_TESTE, []);
    salvarSessaoTeste();
    renderizarApp('/fornecedores');

    expect(await screen.findByText('Nenhum fornecedor cadastrado')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Cadastrar fornecedor' })).toHaveLength(2);
  });

  it('mostra o erro de permissão ou de comunicação informado pela API', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/fornecedores': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    renderizarApp('/fornecedores');

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('Não foi possível carregar os fornecedores');
    expect(alerta).toHaveTextContent('Você não tem permissão para acessar este recurso.');
  });
});

describe('FornecedoresPage — cadastro e edição', () => {
  it('informa os campos obrigatórios sem chamar a API', async () => {
    const fetchFalso = backendDeFornecedores(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar fornecedor' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar fornecedor' });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(within(modal).getByText('A razão social é obrigatória.')).toBeInTheDocument();
    expect(within(modal).getByText('O CNPJ é obrigatório.')).toBeInTheDocument();
    expect(within(modal).getByText('A UF de emissão é obrigatória.')).toBeInTheDocument();
    expect(within(modal).getByText('O tipo do fornecedor é obrigatório.')).toBeInTheDocument();
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);
  });

  it('cadastra o fornecedor com o CNPJ sem máscara e atualiza a listagem', async () => {
    const fetchFalso = backendDeFornecedores(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar fornecedor' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar fornecedor' });
    await usuario.type(within(modal).getByLabelText(/Razão social/), 'Revenda Teste ME');
    await usuario.type(within(modal).getByLabelText(/CNPJ/), '11.444.777/0001-61');
    await selecionar(usuario, modal, /UF de emissão/, 'RJ');
    await selecionar(usuario, modal, /Tipo do fornecedor/, 'Atacadista/revendedor');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Fornecedor "Revenda Teste ME" cadastrado.',
    );
    expect(screen.getByRole('table', { name: 'Fornecedores' })).toHaveTextContent(
      '11.444.777/0001-61',
    );
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toEqual({
      razaoSocial: 'Revenda Teste ME',
      cnpj: '11444777000161',
      uf: 'RJ',
      tipo: 'ATACADISTA',
      prazoPagamentoBase: null,
    });
  });

  it('mostra no campo o CNPJ rejeitado pela API e o conflito de cadastro', async () => {
    let tentativa = 0;
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/fornecedores': () => respostaJson([FABRICANTE]),
      'POST /api/fornecedores': () =>
        ++tentativa === 1
          ? respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
              erros: { cnpj: 'O CNPJ informado é inválido.' },
            })
          : respostaProblema(409, 'Já existe um fornecedor cadastrado com este CNPJ.'),
    });
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar fornecedor' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar fornecedor' });
    await usuario.type(within(modal).getByLabelText(/Razão social/), 'Outro Teste');
    // Formato válido, dígitos verificadores incorretos: só o backend rejeita.
    await usuario.type(within(modal).getByLabelText(/CNPJ/), '11222333000182');
    await selecionar(usuario, modal, /UF de emissão/, 'GO');
    await selecionar(usuario, modal, /Tipo do fornecedor/, 'Fabricante');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    const cnpj = within(modal).getByLabelText(/CNPJ/);
    await waitFor(() => {
      expect(cnpj).toHaveAccessibleDescription(/O CNPJ informado é inválido\./);
    });

    await usuario.clear(cnpj);
    await usuario.type(cnpj, '11222333000181');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(
      await within(modal).findByText(/Já existe um fornecedor cadastrado/),
    ).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Cadastrar fornecedor' })).toBeInTheDocument();
  });

  it('edita o fornecedor com os dados atuais preenchidos', async () => {
    const fetchFalso = backendDeFornecedores(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Editar Atacadista Teste S.A.' }));
    const modal = await screen.findByRole('dialog', { name: 'Editar fornecedor' });
    expect(within(modal).getByLabelText(/Razão social/)).toHaveValue('Atacadista Teste S.A.');
    expect(within(modal).getByLabelText(/CNPJ/)).toHaveValue('12.ABC.345/01DE-35');
    expect(within(modal).getByLabelText(/UF de emissão/)).toHaveValue('MG');
    expect(within(modal).getByLabelText(/Tipo do fornecedor/)).toHaveValue('Atacadista/revendedor');

    await usuario.type(within(modal).getByLabelText(/Prazo de pagamento base/), '28 dias');
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar alterações' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Fornecedor "Atacadista Teste S.A." atualizado.',
    );
    expect(screen.getByRole('table', { name: 'Fornecedores' })).toHaveTextContent('28 dias');
    expect(corpoEnviado(chamadas(fetchFalso, 'PUT')[0]?.[1] ?? {})).toEqual({
      razaoSocial: 'Atacadista Teste S.A.',
      cnpj: '12ABC34501DE35',
      uf: 'MG',
      tipo: 'ATACADISTA',
      prazoPagamentoBase: '28 dias',
    });
  });
});

describe('FornecedoresPage — desativação', () => {
  it('pede confirmação, desativa e remove o fornecedor da listagem', async () => {
    const fetchFalso = backendDeFornecedores(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Desativar Fabricante Teste Ltda' }));
    const modal = await screen.findByRole('dialog', { name: 'Desativar fornecedor' });
    expect(modal).toHaveTextContent('11.222.333/0001-81');
    expect(chamadas(fetchFalso, 'DELETE')).toHaveLength(0);

    await usuario.click(within(modal).getByRole('button', { name: 'Desativar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Fornecedor "Fabricante Teste Ltda" desativado.',
    );
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(screen.getByRole('table', { name: 'Fornecedores' })).not.toHaveTextContent(
      'Fabricante Teste Ltda',
    );
    expect(chamadas(fetchFalso, 'DELETE')).toHaveLength(1);
  });
});
