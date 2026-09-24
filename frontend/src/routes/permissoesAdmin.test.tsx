import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import {
  instalarFetchFalso,
  respostaJson,
  USUARIO_ADMIN_TESTE,
  USUARIO_USER_TESTE,
} from '../test/fetchFalso';
import { renderizarApp } from '../test/renderizar';
import { salvarSessaoTeste } from '../test/sessao';

const ROTAS_ADMIN = [
  '/nfe',
  '/nfe/1',
  '/regras-tributarias',
  '/regras-tributarias/aplicaveis',
  '/parametros-calculo',
  '/usuarios',
];

describe('permissões das áreas administrativas', () => {
  it.each(ROTAS_ADMIN)('USER que acessa %s pela URL vê "Acesso restrito"', async (rota) => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
    });
    renderizarApp(rota);

    expect(await screen.findByRole('heading', { name: 'Acesso restrito' })).toBeInTheDocument();
    // Nenhuma chamada aos endpoints administrativos: só a confirmação da sessão.
    expect(fetchFalso).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('navigation', { name: 'Menu principal' })).not.toHaveTextContent(
      'Parâmetros de cálculo',
    );
  });

  it('ADMIN acessa NF-e, regras tributárias e parâmetros pelo menu', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([]),
      'GET /api/fornecedores': () => respostaJson([]),
      'GET /api/produtos': () => respostaJson([]),
      'GET /api/regras-tributarias': () => respostaJson([]),
      'GET /api/parametros-calculo': () => respostaJson([]),
    });
    renderizarApp('/');
    const usuario = userEvent.setup();
    const menu = await screen.findByRole('navigation', { name: 'Menu principal' });

    await usuario.click(within(menu).getByRole('link', { name: 'NF-e' }));
    expect(await screen.findByText('Nenhuma NF-e importada')).toBeInTheDocument();

    await usuario.click(within(menu).getByRole('link', { name: 'Regras tributárias' }));
    expect(await screen.findByText('Nenhuma regra tributária cadastrada')).toBeInTheDocument();

    await usuario.click(within(menu).getByRole('link', { name: 'Parâmetros de cálculo' }));
    expect(await screen.findByText('Nenhum parâmetro encontrado')).toBeInTheDocument();
    expect(document.title).toBe('Parâmetros de cálculo | Valora');
  });
});
