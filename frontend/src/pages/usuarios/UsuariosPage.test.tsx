import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { tokenStorage } from '../../auth/tokenStorage';
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
import type { Usuario } from '../../types/auth';
import type { CriarUsuarioRequest } from '../../types/usuario';

/** Dados fictícios, exclusivos para testes. */
const INATIVO: Usuario = {
  id: 3,
  nome: 'Usuário Inativo Teste',
  email: 'inativo@teste.local',
  perfil: 'USER',
  ativo: false,
};
const SENHA_TESTE = 'senha-ficticia-123';

/** Backend simulado com estado: a listagem reflete os cadastros. */
function backendDeUsuarios(iniciais: Usuario[] = [USUARIO_ADMIN_TESTE, USUARIO_USER_TESTE]) {
  let usuarios = [...iniciais];
  let proximoId = 100;
  return instalarFetchFalso({
    'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
    'GET /api/usuarios': () => respostaJson(usuarios),
    'POST /api/usuarios': ({ init }) => {
      // A resposta, como a do backend, não inclui a senha.
      const { nome, email, perfil } = corpoEnviado(init) as CriarUsuarioRequest;
      const criado: Usuario = { id: proximoId++, nome, email, perfil, ativo: true };
      usuarios = [...usuarios, criado];
      return respostaJson(criado, 201);
    },
  });
}

function chamadas(fetchFalso: ReturnType<typeof instalarFetchFalso>, metodo: string) {
  return fetchFalso.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === metodo);
}

async function abrirPagina() {
  salvarSessaoTeste();
  renderizarApp('/usuarios');
  return screen.findByRole('table', { name: 'Usuários' });
}

async function abrirCadastro(usuario: UserEvent) {
  await usuario.click(screen.getByRole('button', { name: 'Cadastrar usuário' }));
  return screen.findByRole('dialog', { name: 'Cadastrar usuário' });
}

async function preencher(
  usuario: UserEvent,
  modal: HTMLElement,
  dados: { nome: string; email: string; senha: string; confirmacao?: string; perfil?: string },
) {
  await usuario.type(within(modal).getByLabelText(/^Nome/), dados.nome);
  await usuario.type(within(modal).getByLabelText(/^E-mail/), dados.email);
  await usuario.type(within(modal).getByLabelText(/^Senha/), dados.senha);
  await usuario.type(
    within(modal).getByLabelText(/^Confirmar senha/),
    dados.confirmacao ?? dados.senha,
  );
  if (dados.perfil) {
    await selecionar(usuario, modal, /^Perfil/, dados.perfil);
  }
}

describe('UsuariosPage — acesso', () => {
  it('ADMIN acessa Usuários pelo menu administrativo', async () => {
    backendDeUsuarios();
    salvarSessaoTeste();
    renderizarApp('/');
    const menu = await screen.findByRole('navigation', { name: 'Menu principal' });

    await userEvent.setup().click(within(menu).getByRole('link', { name: 'Usuários' }));

    expect(await screen.findByRole('heading', { name: 'Usuários' })).toBeInTheDocument();
    expect(await screen.findByRole('table', { name: 'Usuários' })).toBeInTheDocument();
    expect(document.title).toBe('Usuários | Valora');
  });

  it('USER não vê Usuários no menu e recebe "Acesso restrito" pela URL, sem chamar a API', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
    });
    renderizarApp('/usuarios');

    expect(await screen.findByRole('heading', { name: 'Acesso restrito' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Menu principal' })).not.toHaveTextContent(
      'Usuários',
    );
    // Nenhuma chamada a /usuarios: só a confirmação da sessão.
    expect(fetchFalso).toHaveBeenCalledTimes(1);
  });
});

describe('UsuariosPage — listagem', () => {
  it('mostra nome, e-mail, perfil e situação, sem nenhuma senha', async () => {
    backendDeUsuarios([USUARIO_ADMIN_TESTE, USUARIO_USER_TESTE, INATIVO]);

    const tabela = await abrirPagina();

    const cabecalhos = within(tabela)
      .getAllByRole('columnheader')
      .map((th) => th.textContent);
    expect(cabecalhos).toEqual(['Nome', 'E-mail', 'Perfil', 'Situação']);
    const linhas = within(tabela).getAllByRole('row');
    expect(linhas).toHaveLength(4);
    expect(linhas[1]).toHaveTextContent('Admin Teste');
    expect(linhas[1]).toHaveTextContent('admin@teste.local');
    expect(linhas[1]).toHaveTextContent('Administrador');
    expect(linhas[1]).toHaveTextContent('Ativo');
    expect(linhas[2]).toHaveTextContent('Usuário Teste');
    expect(linhas[2]).toHaveTextContent('usuario@teste.local');
    expect(linhas[2]).toHaveTextContent('Usuário');
    expect(linhas[3]).toHaveTextContent('Inativo');
    expect(tabela).not.toHaveTextContent(/senha/i);
  });

  it('mostra o carregamento enquanto a listagem não chega', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () => adiada.promessa,
    });
    renderizarApp('/usuarios');

    expect(await screen.findByText('Carregando usuários…')).toBeInTheDocument();

    adiada.responder(respostaJson([USUARIO_ADMIN_TESTE]));
    expect(await screen.findByRole('table', { name: 'Usuários' })).toBeInTheDocument();
    expect(screen.queryByText('Carregando usuários…')).not.toBeInTheDocument();
  });

  it('mostra o estado vazio com a ação de cadastro', async () => {
    backendDeUsuarios([]);
    salvarSessaoTeste();
    renderizarApp('/usuarios');

    expect(await screen.findByText('Nenhum usuário encontrado')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Cadastrar usuário' })).toHaveLength(2);
  });

  it('mostra o erro da API e permite tentar novamente', async () => {
    let falhar = true;
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () =>
        falhar
          ? respostaProblema(500, 'Ocorreu um erro interno. Tente novamente.')
          : respostaJson([USUARIO_ADMIN_TESTE]),
    });
    renderizarApp('/usuarios');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar os usuários');
    expect(alerta).toHaveTextContent('Ocorreu um erro interno. Tente novamente.');

    falhar = false;
    await userEvent.setup().click(within(alerta).getByRole('button', { name: 'Tentar novamente' }));

    expect(await screen.findByRole('table', { name: 'Usuários' })).toHaveTextContent('Admin Teste');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('403 do backend (ex.: perfil alterado) é informado sem exibir dados', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    renderizarApp('/usuarios');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Você não tem permissão para acessar este recurso.');
    expect(screen.queryByRole('table', { name: 'Usuários' })).not.toBeInTheDocument();
  });

  it('401 do backend encerra a sessão e leva ao login', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () => respostaProblema(401, 'Autenticação necessária.'),
    });
    renderizarApp('/usuarios');

    expect(await screen.findByText(/sua sessão expirou/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    expect(tokenStorage.ler()).toBeNull();
  });
});

describe('UsuariosPage — cadastro', () => {
  it('valida os campos antes de enviar e não chama a API com dados inválidos', async () => {
    const fetchFalso = backendDeUsuarios();
    await abrirPagina();
    const usuario = userEvent.setup();
    const modal = await abrirCadastro(usuario);

    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    for (const mensagem of [
      'O nome é obrigatório.',
      'O e-mail é obrigatório.',
      'A senha é obrigatória.',
      'A confirmação da senha é obrigatória.',
      'O perfil é obrigatório (ADMIN ou USER).',
    ]) {
      expect(within(modal).getByText(mensagem)).toBeInTheDocument();
    }

    await preencher(usuario, modal, {
      nome: 'Novo Teste',
      email: 'email-invalido',
      senha: 'curta',
      confirmacao: 'diferente',
    });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(within(modal).getByText('O e-mail informado é inválido.')).toBeInTheDocument();
    expect(
      within(modal).getByText('A senha deve ter entre 8 e 72 caracteres.'),
    ).toBeInTheDocument();
    expect(within(modal).getByText('A confirmação da senha não confere.')).toBeInTheDocument();
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);
  });

  it('cria um USER, fecha o formulário, atualiza a listagem e não exibe a senha', async () => {
    const fetchFalso = backendDeUsuarios();
    await abrirPagina();
    const usuario = userEvent.setup();
    const modal = await abrirCadastro(usuario);

    await preencher(usuario, modal, {
      nome: '  Novo Usuário Teste ',
      email: ' novo.usuario@teste.local ',
      senha: SENHA_TESTE,
      perfil: 'Usuário (USER)',
    });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Usuário "Novo Usuário Teste" cadastrado com o perfil Usuário.',
    );
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    const tabela = screen.getByRole('table', { name: 'Usuários' });
    expect(tabela).toHaveTextContent('novo.usuario@teste.local');
    expect(document.body).not.toHaveTextContent(SENHA_TESTE);
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toEqual({
      nome: 'Novo Usuário Teste',
      email: 'novo.usuario@teste.local',
      senha: SENHA_TESTE,
      perfil: 'USER',
    });

    // Um novo cadastro começa com o formulário vazio.
    const novoModal = await abrirCadastro(usuario);
    expect(within(novoModal).getByLabelText(/^Senha/)).toHaveValue('');
    expect(within(novoModal).getByLabelText(/^E-mail/)).toHaveValue('');
  });

  it('cria um ADMIN', async () => {
    const fetchFalso = backendDeUsuarios();
    await abrirPagina();
    const usuario = userEvent.setup();
    const modal = await abrirCadastro(usuario);

    await preencher(usuario, modal, {
      nome: 'Outro Admin Teste',
      email: 'outro.admin@teste.local',
      senha: SENHA_TESTE,
      perfil: 'Administrador (ADMIN)',
    });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Usuário "Outro Admin Teste" cadastrado com o perfil Administrador.',
    );
    const linhas = within(screen.getByRole('table', { name: 'Usuários' })).getAllByRole('row');
    expect(linhas.at(-1)).toHaveTextContent('Administrador');
    expect(corpoEnviado(chamadas(fetchFalso, 'POST')[0]?.[1] ?? {})).toMatchObject({
      perfil: 'ADMIN',
    });
  });

  it('informa o e-mail já cadastrado (409) e mantém o formulário aberto', async () => {
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () => respostaJson([USUARIO_ADMIN_TESTE, USUARIO_USER_TESTE]),
      'POST /api/usuarios': () =>
        respostaProblema(409, 'Já existe um usuário cadastrado com este e-mail.'),
    });
    await abrirPagina();
    const usuario = userEvent.setup();
    const modal = await abrirCadastro(usuario);

    await preencher(usuario, modal, {
      nome: 'Duplicado Teste',
      email: USUARIO_USER_TESTE.email,
      senha: SENHA_TESTE,
      perfil: 'Usuário (USER)',
    });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'Já existe um usuário cadastrado com este e-mail.',
    );
    expect(screen.getByRole('dialog', { name: 'Cadastrar usuário' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('mostra no próprio campo o erro de validação devolvido pela API', async () => {
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () => respostaJson([USUARIO_ADMIN_TESTE]),
      'POST /api/usuarios': () =>
        respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
          erros: { email: 'O e-mail informado é inválido.' },
        }),
    });
    await abrirPagina();
    const usuario = userEvent.setup();
    const modal = await abrirCadastro(usuario);

    // Aceito pela verificação simples da interface, recusado pelo backend.
    await preencher(usuario, modal, {
      nome: 'Formato Teste',
      email: 'usuario@dominio..local',
      senha: SENHA_TESTE,
      perfil: 'Usuário (USER)',
    });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    const campo = within(modal).getByLabelText(/^E-mail/);
    await waitFor(() => {
      expect(campo).toHaveAccessibleDescription(/O e-mail informado é inválido/);
    });
    expect(campo).toBeInvalid();
    expect(within(modal).getByRole('alert')).toHaveTextContent(
      'Dados inválidos. Corrija os campos informados.',
    );
  });

  it('informa a falta de permissão (403) ao cadastrar', async () => {
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/usuarios': () => respostaJson([USUARIO_ADMIN_TESTE]),
      'POST /api/usuarios': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    await abrirPagina();
    const usuario = userEvent.setup();
    const modal = await abrirCadastro(usuario);

    await preencher(usuario, modal, {
      nome: 'Sem Permissão Teste',
      email: 'sem.permissao@teste.local',
      senha: SENHA_TESTE,
      perfil: 'Usuário (USER)',
    });
    await usuario.click(within(modal).getByRole('button', { name: 'Cadastrar' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'Você não tem permissão para acessar este recurso.',
    );
  });
});
