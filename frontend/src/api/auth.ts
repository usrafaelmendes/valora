import type {
  ConfiguracaoInicialRequest,
  LoginRequest,
  LoginResponse,
  StatusConfiguracaoInicial,
  Usuario,
} from '../types/auth';
import { http } from './httpClient';

/** Chave do TanStack Query do status da configuração inicial. */
export const CHAVE_CONFIGURACAO_INICIAL = ['configuracao-inicial'] as const;

/** Endpoints de autenticação do backend (AuthController). */
export const authApi = {
  /** Rota pública: 401 aqui significa credenciais inválidas, não sessão expirada. */
  login(dados: LoginRequest): Promise<LoginResponse> {
    return http.post<LoginResponse>('/auth/login', dados, { autenticada: false });
  },

  /** Usuário do token atual; confirma que a sessão continua válida. */
  me(signal?: AbortSignal): Promise<Usuario> {
    return http.get<Usuario>('/auth/me', { signal });
  },

  /** Rota pública: informa se já existe algum usuário (sistema configurado). */
  statusConfiguracaoInicial(signal?: AbortSignal): Promise<StatusConfiguracaoInicial> {
    return http.get<StatusConfiguracaoInicial>('/auth/configuracao-inicial', {
      autenticada: false,
      signal,
    });
  },

  /** Rota pública: cria o primeiro ADMIN; o backend recusa (409) se já houver usuário. */
  configurarPrimeiroAdmin(dados: ConfiguracaoInicialRequest): Promise<Usuario> {
    return http.post<Usuario>('/auth/configuracao-inicial', dados, { autenticada: false });
  },
};
