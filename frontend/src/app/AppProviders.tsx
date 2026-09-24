import { MantineProvider } from '@mantine/core';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { AuthProvider } from '../auth/AuthProvider';
import { theme } from '../config/theme';

interface AppProvidersProps {
  queryClient: QueryClient;
  children: ReactNode;
}

/** Provedores globais, na ordem de dependência (AuthProvider usa o QueryClient). */
export function AppProviders({ queryClient, children }: AppProvidersProps) {
  return (
    <MantineProvider theme={theme} defaultColorScheme="light">
      <QueryClientProvider client={queryClient}>
        <AuthProvider>{children}</AuthProvider>
      </QueryClientProvider>
    </MantineProvider>
  );
}
