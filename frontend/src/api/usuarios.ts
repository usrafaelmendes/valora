import type { Usuario } from '../types/auth';
import type { CriarUsuarioRequest } from '../types/usuario';
import { http } from './httpClient';

/** Chave do TanStack Query da listagem; invalidada após o cadastro. */
export const CHAVE_USUARIOS = ['usuarios'] as const;

/** Endpoints de usuários (UsuarioController). Restritos ao ADMIN pelo backend. */
export const usuariosApi = {
  /** Todos os usuários, inclusive inativos, ordenados pelo id. */
  listar(signal?: AbortSignal): Promise<Usuario[]> {
    return http.get<Usuario[]>('/usuarios', { signal });
  },

  /** O backend guarda somente o hash da senha e recusa e-mail já cadastrado (409). */
  criar(dados: CriarUsuarioRequest): Promise<Usuario> {
    return http.post<Usuario>('/usuarios', dados);
  },
};
