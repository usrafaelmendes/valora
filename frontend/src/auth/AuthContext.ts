import { createContext } from 'react';
import type { LoginRequest, Usuario } from '../types/auth';

/**
 * - `verificando`: há token salvo e a sessão está sendo confirmada no backend (após reload);
 * - `autenticado`: usuário e perfil conhecidos;
 * - `anonimo`: sem sessão.
 */
export type SituacaoSessao = 'verificando' | 'autenticado' | 'anonimo';

/** Por que a sessão terminou sem o usuário pedir; usado para informar na tela de login. */
export type MotivoFimSessao = 'expirada' | null;

export interface AuthContextValue {
  situacao: SituacaoSessao;
  usuario: Usuario | null;
  isAdmin: boolean;
  motivoFimSessao: MotivoFimSessao;
  login: (dados: LoginRequest) => Promise<Usuario>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
