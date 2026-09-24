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
  USUARIO_USER_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { salvarSessaoTeste } from '../../test/sessao';
import type { Produto, ProdutoRequest } from '../../types/produto';

/** Dados fictícios, exclusivos para testes. */
const PRODUTO_A: Produto = {
  id: 1,
  nome: 'Produto Teste A',
  descricao: 'Produto fictício para testes',
  gtin: '7891234567895',
  ativo: true,
};
const PRODUTO_B: Produto = {
  id: 2,
  nome: 'Produto Teste B',
  descricao: null,
  gtin: null,
  ativo: true,
};

type Usuario = typeof USUARIO_ADMIN_TESTE | typeof USUARIO_USER_TESTE;

/** Backend simulado com estado: a listagem reflete cadastros, edições e desativações. */
function backendDeProdutos(usuario: Usuario, iniciais: Produto[] = [PRODUTO_A, PRODUTO_B]) {
  let produtos = [...iniciais];
  let proximoId = 100;
  const fetchFalso = instalarFetchFalso({
    'GET /api/auth/me': () => respostaJson(usuario),
    'GET /api/produtos': () => respostaJson(produtos),
    'POST /api/produtos': ({ init }) => {
      const dados = corpoEnviado(init) as ProdutoRequest;
      const criado: Produto = { id: proximoId++, ...dados, ativo: true };
      produtos = [...produtos, criado];
      return respostaJson(criado, 201);
    },
    'PUT /api/produtos/1': ({ init }) => {
      const atualizado: Produto = { ...PRODUTO_A, ...(corpoEnviado(init) as ProdutoRequest) };
      produtos = produtos.map((p) => (p.id === 1 ? atualizado : p));
      return respostaJson(atualizado);
    },
    'DELETE /api/produtos/2': () => {
      produtos = produtos.filter((p) => p.id !== 2);
      return new Response(null, { status: 204 });
    },
  });
  return fetchFalso;
}

function chamadas(fetchFalso: ReturnType<typeof instalarFetchFalso>, metodo: string) {
  return fetchFalso.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === metodo);
}

async function abrirPagina() {
  salvarSessaoTeste();
  renderizarApp('/produtos');
  return screen.findByRole('table', { name: 'Produtos' });
}

describe('ProdutosPage — consulta', () => {
  it('USER vê a listagem com os dados do produto, sem ações de escrita', async () => {
    backendDeProdutos(USUARIO_USER_TESTE);

    const tabela = await abrirPagina();

    expect(screen.getByRole('heading', { name: 'Produtos' })).toBeInTheDocument();
    const linhas = within(tabela).getAllByRole('row');
    expect(linhas).toHaveLength(3);
    expect(linhas[1]).toHaveTextContent('Produto Teste A');
    expect(linhas[1]).toHaveTextContent('Produto fictício para testes');
    expect(linhas[1]).toHaveTextContent('7891234567895');
    expect(linhas[2]).toHaveTextContent('Produto Teste B');
    expect(linhas[2]).toHaveTextContent('Não informado');
    expect(within(tabela).queryByRole('columnheader', { name: 'Ações' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /cadastrar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /editar|desativar/i })).not.toBeInTheDocument();
  });

  it('ADMIN vê as ações de cadastro, edição e desativação', async () => {
    backendDeProdutos(USUARIO_ADMIN_TESTE);

    await abrirPagina();

    expect(screen.getByRole('button', { name: 'Cadastrar produto' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Editar Produto Teste A' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Desativar Produto Teste B' })).toBeInTheDocument();
  });

  it('mostra o carregamento enquanto a listagem não chega', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/produtos': () => adiada.promessa,
    });
    renderizarApp('/produtos');

    expect(await screen.findByText('Carregando produtos…')).toBeInTheDocument();

    adiada.responder(respostaJson([PRODUTO_B]));
    expect(await screen.findByRole('table', { name: 'Produtos' })).toBeInTheDocument();
    expect(screen.queryByText('Carregando produtos…')).not.toBeInTheDocument();
  });

  it('mostra o estado vazio com a ação de cadastro para o ADMIN', async () => {
    backendDeProdutos(USUARIO_ADMIN_TESTE, []);
    salvarSessaoTeste();
    renderizarApp('/produtos');

    expect(await screen.findByText('Nenhum produto cadastrado')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Cadastrar produto' })).toHaveLength(2);
  });

  it('mostra o estado vazio sem ação para o USER', async () => {
    backendDeProdutos(USUARIO_USER_TESTE, []);
    salvarSessaoTeste();
    renderizarApp('/produtos');

    expect(await screen.findByText(/peça a um administrador/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cadastrar produto' })).not.toBeInTheDocument();
  });

  it('mostra o erro da API e permite tentar novamente', async () => {
    let falhar = true;
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/produtos': () =>
        falhar
          ? respostaProblema(500, 'Ocorreu um erro interno. Tente novamente.')
          : respostaJson([PRODUTO_B]),
    });
    renderizarApp('/produtos');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar os produtos');
    expect(alerta).toHaveTextContent('Ocorreu um erro interno. Tente novamente.');

    falhar = false;
    await userEvent.setup().click(within(alerta).getByRole('button', { name: 'Tentar novamente' }));

    expect(await screen.findByRole('table', { name: 'Produtos' })).toHaveTextContent(
      'Produto Teste B',
    );
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('ProdutosPage — cadastro', () => {
  it('valida os campos antes de enviar e não chama a API com dados inválidos', async () => {
    const fetchFalso = backendDeProdutos(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar produto' });
    await usuario.type(within(modal).getByLabelText(/GTIN\/EAN/), '123');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(within(modal).getByText('O nome é obrigatório.')).toBeInTheDocument();
    expect(within(modal).getByText(/GTIN\/EAN informado é inválido/)).toBeInTheDocument();
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);

    // O erro some quando o campo é corrigido.
    await usuario.type(within(modal).getByLabelText(/Nome/), 'Placa-mãe Teste');
    expect(within(modal).queryByText('O nome é obrigatório.')).not.toBeInTheDocument();
  });

  it('cadastra o produto, fecha o formulário e atualiza a listagem', async () => {
    const fetchFalso = backendDeProdutos(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar produto' });
    await usuario.type(within(modal).getByLabelText(/Nome/), '  Produto Teste C ');
    await usuario.type(within(modal).getByLabelText(/Descrição/), 'Módulo fictício');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Produto "Produto Teste C" cadastrado.',
    );
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(screen.getByRole('table', { name: 'Produtos' })).toHaveTextContent('Produto Teste C');
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toEqual({
      nome: 'Produto Teste C',
      descricao: 'Módulo fictício',
      gtin: null,
    });
  });

  it('mostra no formulário o conflito informado pela API', async () => {
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/produtos': () => respostaJson([PRODUTO_A]),
      'POST /api/produtos': () =>
        respostaProblema(409, 'Já existe um produto desativado cadastrado com este nome.'),
    });
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar produto' });
    await usuario.type(within(modal).getByLabelText(/Nome/), 'Produto Antigo');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'Já existe um produto desativado cadastrado com este nome.',
    );
    expect(screen.getByRole('dialog', { name: 'Cadastrar produto' })).toBeInTheDocument();
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(1);
  });

  it('mostra no próprio campo o erro de validação devolvido pela API', async () => {
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/produtos': () => respostaJson([PRODUTO_B]),
      'POST /api/produtos': () =>
        respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
          erros: { gtin: 'O GTIN/EAN informado é inválido (use 8, 12, 13 ou 14 dígitos).' },
        }),
    });
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar produto' }));
    const modal = await screen.findByRole('dialog', { name: 'Cadastrar produto' });
    await usuario.type(within(modal).getByLabelText(/Nome/), 'HD Teste');
    // Formato válido, mas dígito verificador incorreto: só o backend rejeita.
    await usuario.type(within(modal).getByLabelText(/GTIN\/EAN/), '7891234567890');
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    const campo = within(modal).getByLabelText(/GTIN\/EAN/);
    await waitFor(() => {
      expect(campo).toHaveAccessibleDescription(/GTIN\/EAN informado é inválido/);
    });
    expect(campo).toBeInvalid();
    expect(within(modal).getByRole('alert')).toHaveTextContent(
      'Dados inválidos. Corrija os campos informados.',
    );
  });
});

describe('ProdutosPage — edição e desativação', () => {
  it('edita o produto com os dados atuais preenchidos e atualiza a listagem', async () => {
    const fetchFalso = backendDeProdutos(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Editar Produto Teste A' }));
    const modal = await screen.findByRole('dialog', { name: 'Editar produto' });
    const nome = within(modal).getByLabelText(/Nome/);
    expect(nome).toHaveValue('Produto Teste A');
    expect(within(modal).getByLabelText(/GTIN\/EAN/)).toHaveValue('7891234567895');

    await usuario.clear(nome);
    await usuario.type(nome, 'Produto Teste A Editado');
    await usuario.click(within(modal).getByRole('button', { name: 'Salvar alterações' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Produto "Produto Teste A Editado" atualizado.',
    );
    expect(screen.getByRole('table', { name: 'Produtos' })).toHaveTextContent(
      'Produto Teste A Editado',
    );
    expect(corpoEnviado(chamadas(fetchFalso, 'PUT')[0]?.[1] ?? {})).toEqual({
      nome: 'Produto Teste A Editado',
      descricao: 'Produto fictício para testes',
      gtin: '7891234567895',
    });
  });

  it('cancelar a confirmação não desativa o produto', async () => {
    const fetchFalso = backendDeProdutos(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Desativar Produto Teste B' }));
    const modal = await screen.findByRole('dialog', { name: 'Desativar produto' });
    expect(modal).toHaveTextContent('Produto Teste B');
    await usuario.click(within(modal).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(chamadas(fetchFalso, 'DELETE')).toHaveLength(0);
    expect(screen.getByRole('table', { name: 'Produtos' })).toHaveTextContent('Produto Teste B');
  });

  it('desativa após confirmação e remove o produto da listagem', async () => {
    const fetchFalso = backendDeProdutos(USUARIO_ADMIN_TESTE);
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Desativar Produto Teste B' }));
    const modal = await screen.findByRole('dialog', { name: 'Desativar produto' });
    await usuario.click(within(modal).getByRole('button', { name: 'Desativar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Produto "Produto Teste B" desativado.',
    );
    expect(screen.getByRole('table', { name: 'Produtos' })).not.toHaveTextContent(
      'Produto Teste B',
    );
    expect(chamadas(fetchFalso, 'DELETE')).toHaveLength(1);
  });

  it('mostra o erro da API na confirmação e atualiza a listagem', async () => {
    let produtos = [PRODUTO_A, PRODUTO_B];
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/produtos': () => respostaJson(produtos),
      'DELETE /api/produtos/2': () => {
        // Outro administrador desativou o produto antes.
        produtos = [PRODUTO_A];
        return respostaProblema(404, 'Produto não encontrado.');
      },
    });
    await abrirPagina();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Desativar Produto Teste B' }));
    const modal = await screen.findByRole('dialog', { name: 'Desativar produto' });
    await usuario.click(within(modal).getByRole('button', { name: 'Desativar' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent('Produto não encontrado.');
    await waitFor(() => {
      expect(screen.getByRole('table', { name: 'Produtos', hidden: true })).not.toHaveTextContent(
        'Produto Teste B',
      );
    });
  });
});
