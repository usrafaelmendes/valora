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

describe('navegação para produtos e fornecedores', () => {
  it('USER acessa Produtos e Fornecedores pelo menu, sem o menu administrativo', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_USER_TESTE),
      'GET /api/produtos': () => respostaJson([]),
      'GET /api/fornecedores': () => respostaJson([]),
    });
    renderizarApp('/');
    const usuario = userEvent.setup();
    const menu = await screen.findByRole('navigation', { name: 'Menu principal' });
    expect(menu).not.toHaveTextContent('Administração');

    await usuario.click(within(menu).getByRole('link', { name: 'Produtos' }));
    expect(await screen.findByRole('heading', { name: 'Produtos' })).toBeInTheDocument();
    expect(within(menu).getByRole('link', { name: 'Produtos' })).toHaveAttribute(
      'aria-current',
      'page',
    );

    await usuario.click(within(menu).getByRole('link', { name: 'Fornecedores' }));
    expect(await screen.findByRole('heading', { name: 'Fornecedores' })).toBeInTheDocument();
    expect(await screen.findByText('Nenhum fornecedor cadastrado')).toBeInTheDocument();
  });

  it('ADMIN acessa as páginas diretamente pela URL', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/fornecedores': () => respostaJson([]),
    });
    renderizarApp('/fornecedores');

    expect(await screen.findByRole('heading', { name: 'Fornecedores' })).toBeInTheDocument();
    expect(document.title).toBe('Fornecedores | Valora');
  });
});
