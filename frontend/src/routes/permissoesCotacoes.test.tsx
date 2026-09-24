import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { tokenStorage } from '../auth/tokenStorage';
import { COMPARACAO_RESUMO_TESTE, COMPARACAO_TESTE } from '../test/dadosFicticios';
import {
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  USUARIO_ADMIN_TESTE,
  USUARIO_USER_TESTE,
} from '../test/fetchFalso';
import { renderizarApp } from '../test/renderizar';
import { salvarSessaoTeste } from '../test/sessao';

function rotasComuns(perfil: object) {
  return {
    'GET /api/auth/me': () => respostaJson(perfil),
    'GET /api/cotacoes': () => respostaJson([]),
    'GET /api/produtos': () => respostaJson([]),
    'GET /api/fornecedores': () => respostaJson([]),
    'GET /api/nfe': () => respostaJson([]),
    'GET /api/comparacoes/7': () => respostaJson(COMPARACAO_TESTE),
    'GET /api/cotacoes/3/comparacoes': () => respostaJson([COMPARACAO_RESUMO_TESTE]),
  };
}

describe('permissões de cotações e comparações', () => {
  it.each([
    ['USER', USUARIO_USER_TESTE],
    ['ADMIN', USUARIO_ADMIN_TESTE],
  ])('%s acessa cotações, nova cotação e comparação pelo menu e pela URL', async (_, perfil) => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComuns(perfil));
    renderizarApp('/');
    const usuario = userEvent.setup();
    const menu = await screen.findByRole('navigation', { name: 'Menu principal' });

    await usuario.click(within(menu).getByRole('link', { name: 'Cotações' }));
    expect(await screen.findByText('Nenhuma cotação registrada')).toBeInTheDocument();

    const [novaCotacao] = screen.getAllByRole('link', { name: 'Nova cotação' });
    if (!novaCotacao) {
      throw new Error('Link "Nova cotação" não encontrado.');
    }
    await usuario.click(novaCotacao);
    expect(await screen.findByRole('heading', { name: 'Nova cotação' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Acesso restrito' })).not.toBeInTheDocument();
  });

  it('USER abre uma comparação e o menu mantém "Cotações" ativo', async () => {
    salvarSessaoTeste();
    instalarFetchFalso(rotasComuns(USUARIO_USER_TESTE));
    renderizarApp('/comparacoes/7');

    expect(await screen.findByRole('heading', { name: 'Comparação nº 7' })).toBeInTheDocument();
    const menu = screen.getByRole('navigation', { name: 'Menu principal' });
    expect(within(menu).getByRole('link', { name: 'Cotações' })).toHaveAttribute(
      'data-active',
      'true',
    );
  });

  it('sem sessão, a rota protegida leva ao login', async () => {
    instalarFetchFalso({});
    renderizarApp('/comparacoes/7');

    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
  });

  it('401 em chamada de cotações encerra a sessão e leva ao login', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/cotacoes': () => respostaProblema(401, 'Token inválido ou expirado.'),
      'GET /api/produtos': () => respostaJson([]),
    });
    renderizarApp('/cotacoes');

    expect(await screen.findByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    expect(tokenStorage.ler()).toBeNull();
  });

  it('403 do backend é exibido sem esconder a mensagem', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/comparacoes/7': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    renderizarApp('/comparacoes/7');

    expect(await screen.findByRole('alert', {}, { timeout: 3000 })).toHaveTextContent(
      'Você não tem permissão para acessar este recurso.',
    );
  });
});
