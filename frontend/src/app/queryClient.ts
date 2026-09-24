import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/ApiError';

/** Erros 4xx dependem da requisição ou da sessão: repetir não muda o resultado. */
export function deveRepetir(tentativas: number, erro: unknown): boolean {
  if (erro instanceof ApiError && erro.status >= 400 && erro.status < 500) {
    return false;
  }
  return tentativas < 1;
}

export function criarQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: deveRepetir,
        refetchOnWindowFocus: false,
        staleTime: 30_000,
      },
      mutations: {
        retry: false,
      },
    },
  });
}
