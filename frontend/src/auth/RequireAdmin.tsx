import { Outlet } from 'react-router';
import { AcessoNegadoPage } from '../pages/AcessoNegadoPage';
import { useAuth } from './useAuth';

/**
 * Rotas exclusivas do ADMIN. Deve ficar dentro de RequireAuth.
 * É apenas uma proteção de interface: o backend também nega essas operações ao USER (403).
 */
export function RequireAdmin() {
  const { isAdmin } = useAuth();
  return isAdmin ? <Outlet /> : <AcessoNegadoPage />;
}
