import { tokenStorage } from '../auth/tokenStorage';
import { expiraEmFuturo, TOKEN_TESTE } from './fetchFalso';

/** Simula uma sessão salva antes do reload; o perfil vem do GET /api/auth/me simulado. */
export function salvarSessaoTeste(): void {
  tokenStorage.salvar({ token: TOKEN_TESTE, expiraEm: expiraEmFuturo() });
}
