import { Navigate, Outlet, useLocation } from 'react-router';
import { CarregandoPagina } from '../components/CarregandoPagina';
import { ROTAS, type EstadoRedirecionamentoLogin } from '../routes/rotas';
import { useAuth } from './useAuth';

/** Rotas que exigem sessão: sem ela, redireciona ao login guardando a página pedida. */
export function RequireAuth() {
  const { situacao } = useAuth();
  const location = useLocation();

  if (situacao === 'verificando') {
    return <CarregandoPagina mensagem="Verificando sessão…" />;
  }
  if (situacao === 'anonimo') {
    const estado: EstadoRedirecionamentoLogin = {
      de: `${location.pathname}${location.search}${location.hash}`,
    };
    return <Navigate to={ROTAS.login} replace state={estado} />;
  }
  return <Outlet />;
}
