import { useState } from 'react';
import { BrowserRouter } from 'react-router';
import { AppRoutes } from '../routes/AppRoutes';
import { AppProviders } from './AppProviders';
import { criarQueryClient } from './queryClient';

export function App() {
  const [queryClient] = useState(criarQueryClient);
  return (
    <BrowserRouter>
      <AppProviders queryClient={queryClient}>
        <AppRoutes />
      </AppProviders>
    </BrowserRouter>
  );
}
