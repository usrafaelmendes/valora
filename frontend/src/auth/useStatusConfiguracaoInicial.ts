import { useQuery } from '@tanstack/react-query';
import { ApiError } from '../api/ApiError';
import { authApi, CHAVE_CONFIGURACAO_INICIAL } from '../api/auth';
import { deveRepetir } from '../app/queryClient';

/**
 * Enquanto o backend não responde, a consulta é repetida a cada segundo por até 2 minutos: no
 * Valora desktop o backend local é iniciado junto com o aplicativo e leva alguns segundos para subir.
 */
const TENTATIVAS_SEM_RESPOSTA = 120;
const INTERVALO_SEM_RESPOSTA_MS = 1_000;

function aguardandoServidor(erro: unknown): boolean {
  return erro instanceof ApiError && erro.semResposta;
}

/** Se o sistema já tem usuários; consultado somente nas telas públicas (login e configuração inicial). */
export function useStatusConfiguracaoInicial() {
  return useQuery({
    queryKey: CHAVE_CONFIGURACAO_INICIAL,
    queryFn: ({ signal }) => authApi.statusConfiguracaoInicial(signal),
    retry: (falhas, erro) =>
      aguardandoServidor(erro) ? falhas < TENTATIVAS_SEM_RESPOSTA : deveRepetir(falhas, erro),
    retryDelay: (tentativa, erro) =>
      aguardandoServidor(erro)
        ? INTERVALO_SEM_RESPOSTA_MS
        : Math.min(1_000 * 2 ** tentativa, 30_000),
  });
}

/** Verdadeiro enquanto a consulta ainda repete porque o servidor não respondeu. */
export function aguardandoServidorLocal(status: ReturnType<typeof useStatusConfiguracaoInicial>) {
  return status.isPending && aguardandoServidor(status.failureReason);
}
