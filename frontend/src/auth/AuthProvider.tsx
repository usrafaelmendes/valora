import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { ApiError } from '../api/ApiError';
import { authApi } from '../api/auth';
import { configurarHttpClient } from '../api/httpClient';
import type { LoginRequest, Usuario } from '../types/auth';
import {
  AuthContext,
  type AuthContextValue,
  type MotivoFimSessao,
  type SituacaoSessao,
} from './AuthContext';
import { tokenStorage, type TokenArmazenado } from './tokenStorage';

interface EstadoSessao {
  situacao: SituacaoSessao;
  usuario: Usuario | null;
  motivoFimSessao: MotivoFimSessao;
}

/** Maior atraso aceito por setTimeout (~24,8 dias). */
const ATRASO_MAXIMO_MS = 2_147_483_647;

/**
 * Mantém a sessão do usuário: login, logout, recuperação após reload e fim da sessão
 * quando o token expira ou o backend responde 401. O perfil (ADMIN/USER) vem sempre
 * do backend (login ou GET /auth/me); o frontend não decodifica o JWT.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  // Token salvo antes do reload, lido uma única vez na montagem.
  const [tokenInicial] = useState(() => tokenStorage.ler());
  const tokenRef = useRef<TokenArmazenado | null>(tokenInicial);
  const [estado, setEstado] = useState<EstadoSessao>({
    situacao: tokenInicial ? 'verificando' : 'anonimo',
    usuario: null,
    motivoFimSessao: null,
  });

  const encerrarSessao = useCallback(
    (motivo: MotivoFimSessao) => {
      tokenStorage.limpar();
      tokenRef.current = null;
      // Dados carregados na sessão anterior não podem aparecer para o próximo usuário.
      queryClient.clear();
      setEstado({ situacao: 'anonimo', usuario: null, motivoFimSessao: motivo });
    },
    [queryClient],
  );

  // Liga o cliente HTTP à sessão: token nas requisições e fim da sessão diante de 401.
  useEffect(
    () =>
      configurarHttpClient({
        obterToken: () => tokenRef.current?.token ?? null,
        aoNaoAutorizado: () => {
          if (tokenRef.current) {
            encerrarSessao('expirada');
          }
        },
      }),
    [encerrarSessao],
  );

  // Após reload: confirma no backend o token salvo e obtém o usuário e o perfil atuais.
  const verificando = estado.situacao === 'verificando';
  useEffect(() => {
    if (!verificando) {
      return;
    }
    const controle = new AbortController();
    authApi
      .me(controle.signal)
      .then((usuario) => {
        setEstado({ situacao: 'autenticado', usuario, motivoFimSessao: null });
      })
      .catch((erro: unknown) => {
        if (controle.signal.aborted) {
          return;
        }
        // 401 já encerrou a sessão via aoNaoAutorizado; outros erros também exigem novo login.
        encerrarSessao(erro instanceof ApiError && erro.status === 401 ? 'expirada' : null);
      });
    return () => {
      controle.abort();
    };
  }, [verificando, encerrarSessao]);

  // Encerra a sessão no instante de expiração do token, sem esperar um 401.
  const autenticado = estado.situacao === 'autenticado';
  useEffect(() => {
    const expiraEm = tokenRef.current?.expiraEm;
    if (!autenticado || !expiraEm) {
      return;
    }
    const restante = Math.min(Math.max(Date.parse(expiraEm) - Date.now(), 0), ATRASO_MAXIMO_MS);
    const timer = setTimeout(() => {
      encerrarSessao('expirada');
    }, restante);
    return () => {
      clearTimeout(timer);
    };
  }, [autenticado, encerrarSessao]);

  const login = useCallback(async (dados: LoginRequest): Promise<Usuario> => {
    const resposta = await authApi.login(dados);
    const token = { token: resposta.token, expiraEm: resposta.expiraEm };
    tokenStorage.salvar(token);
    tokenRef.current = token;
    setEstado({ situacao: 'autenticado', usuario: resposta.usuario, motivoFimSessao: null });
    return resposta.usuario;
  }, []);

  const logout = useCallback(() => {
    encerrarSessao(null);
  }, [encerrarSessao]);

  const valor = useMemo<AuthContextValue>(
    () => ({
      situacao: estado.situacao,
      usuario: estado.usuario,
      isAdmin: estado.usuario?.perfil === 'ADMIN',
      motivoFimSessao: estado.motivoFimSessao,
      login,
      logout,
    }),
    [estado, login, logout],
  );

  return <AuthContext value={valor}>{children}</AuthContext>;
}
