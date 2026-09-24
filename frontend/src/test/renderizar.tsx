import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { AppProviders } from '../app/AppProviders';
import { criarQueryClient } from '../app/queryClient';
import { AppRoutes } from '../routes/AppRoutes';

/** Renderiza a aplicação completa (provedores + rotas) iniciando na rota informada. */
export function renderizarApp(rotaInicial = '/') {
  const queryClient = criarQueryClient();
  return {
    queryClient,
    ...render(
      <MemoryRouter initialEntries={[rotaInicial]}>
        <AppProviders queryClient={queryClient}>
          <AppRoutes />
        </AppProviders>
      </MemoryRouter>,
    ),
  };
}
