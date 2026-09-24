import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderizarApp } from '../test/renderizar';
import {
  autorizacaoEnviada,
  expiraEmFuturo,
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  TOKEN_TESTE,
  USUARIO_ADMIN_TESTE,
  USUARIO_USER_TESTE,
} from '../test/fetchFalso';
import { tokenStorage } from './tokenStorage';

function sessaoSalva() {
  tokenStorage.salvar({ token: TOKEN_TESTE, expiraEm: expiraEmFuturo() });
}

async function preencherLogin(email: string, senha: string) {
  const usuario = userEvent.setup();
  await usuario.type(screen.getByLabelText(/e-mail/i), email);
  await usuario.type(screen.getByLabelText(/senha/i), senha);
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }));
  return usuario;
}

describe('autenticação', () => {
  it('login válido armazena o token e mostra o usuário e o perfil', async () => {
    const expiraEm = expiraEmFuturo();
    const fetchFalso = instalarFetchFalso({
      'POST /api/auth/login': () =>
        respostaJson({
          token: TOKEN_TESTE,
          tipo: 'Bearer',
          expiraEm,
          usuario: USUARIO_ADMIN_TESTE,
        }),
    });
    renderizarApp('/login');

    await preencherLogin(USUARIO_ADMIN_TESTE.email, 'senha-de-teste');

    expect(await screen.findByText('Olá, Admin Teste')).toBeInTheDocument();
    expect(screen.getByText('Administrador')).toBeInTheDocument();
    expect(tokenStorage.ler()).toEqual({ token: TOKEN_TESTE, expiraEm });
    // A tela de login também consulta a configuração inicial: localiza a chamada do login.
    const corpo = fetchFalso.mock.calls.find(([, init]) => init?.method === 'POST')?.[1]?.body;
    expect(corpo).toBe(
      JSON.stringify({ email: USUARIO_ADMIN_TESTE.email, senha: 'senha-de-teste' }),
    );
  });

  it('login inválido mostra a mensagem do backend e não cria sessão', async () => {
    instalarFetchFalso({
      'POST /api/auth/login': () => respostaProblema(401, 'E-mail ou senha inválidos.'),
    });
    renderizarApp('/login');

    await preencherLogin('errado@teste.local', 'senha-errada');

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.');
    expect(screen.queryByText(/sessão expirou/i)).not.toBeInTheDocument();
    expect(tokenStorage.ler()).toBeNull();
  });

  it('recupera a sessão após reload consultando /auth/me com o token salvo', async () => {
    sessaoSalva();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
    });

    renderizarApp('/');

    expect(await screen.findByText('Olá, Usuário Teste')).toBeInTheDocument();
    expect(autorizacaoEnviada(fetchFalso.mock.calls[0]?.[1])).toBe(`Bearer ${TOKEN_TESTE}`);
  });

  it('logout limpa o token e volta ao login', async () => {
    sessaoSalva();
    instalarFetchFalso({ 'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE) });
    renderizarApp('/');
    await screen.findByText('Olá, Admin Teste');

    await userEvent.setup().click(screen.getByRole('button', { name: 'Sair' }));

    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    expect(tokenStorage.ler()).toBeNull();
    expect(screen.queryByText(/sessão expirou/i)).not.toBeInTheDocument();
  });

  it('401 com token salvo encerra a sessão e informa na tela de login', async () => {
    sessaoSalva();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaProblema(401, 'Autenticação necessária.'),
    });

    renderizarApp('/produtos');

    expect(await screen.findByText(/sua sessão expirou/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    expect(tokenStorage.ler()).toBeNull();
  });
});

describe('proteção de rotas', () => {
  it('redireciona ao login sem sessão e volta à página pedida após entrar', async () => {
    instalarFetchFalso({
      'POST /api/auth/login': () =>
        respostaJson({
          token: TOKEN_TESTE,
          tipo: 'Bearer',
          expiraEm: expiraEmFuturo(),
          usuario: USUARIO_USER_TESTE,
        }),
    });
    renderizarApp('/cotacoes');

    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    await preencherLogin(USUARIO_USER_TESTE.email, 'senha-de-teste');

    expect(await screen.findByRole('heading', { name: 'Cotações' })).toBeInTheDocument();
  });

  it('bloqueia rota de ADMIN para USER e oculta o menu administrativo', async () => {
    sessaoSalva();
    instalarFetchFalso({ 'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE) });

    renderizarApp('/regras-tributarias');

    expect(await screen.findByRole('heading', { name: 'Acesso restrito' })).toBeInTheDocument();
    const menu = screen.getByRole('navigation', { name: 'Menu principal' });
    expect(menu).not.toHaveTextContent('Administração');
    expect(menu).not.toHaveTextContent('NF-e');
    expect(menu).toHaveTextContent('Cotações');
  });

  it('permite rota de ADMIN para ADMIN', async () => {
    sessaoSalva();
    instalarFetchFalso({ 'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE) });

    renderizarApp('/regras-tributarias');

    expect(await screen.findByRole('heading', { name: 'Regras tributárias' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Menu principal' })).toHaveTextContent('NF-e');
  });

  it('mostra página não encontrada para rota inexistente', async () => {
    sessaoSalva();
    instalarFetchFalso({ 'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE) });

    renderizarApp('/rota-inexistente');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument();
    });
  });
});
