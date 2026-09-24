import { Route, Routes } from 'react-router';
import { RequireAdmin } from '../auth/RequireAdmin';
import { RequireAuth } from '../auth/RequireAuth';
import { AppLayout } from '../layouts/AppLayout';
import { ConfiguracaoInicialPage } from '../pages/configuracao-inicial/ConfiguracaoInicialPage';
import { ComparacaoPage } from '../pages/cotacoes/ComparacaoPage';
import { CotacaoDetalhePage } from '../pages/cotacoes/CotacaoDetalhePage';
import { CotacoesPage } from '../pages/cotacoes/CotacoesPage';
import { NovaCotacaoPage } from '../pages/cotacoes/NovaCotacaoPage';
import { FornecedoresPage } from '../pages/fornecedores/FornecedoresPage';
import { InicioPage } from '../pages/InicioPage';
import { LoginPage } from '../pages/LoginPage';
import { NaoEncontradaPage } from '../pages/NaoEncontradaPage';
import { NfeDetalhePage } from '../pages/nfe/NfeDetalhePage';
import { NfePage } from '../pages/nfe/NfePage';
import { ParametrosCalculoPage } from '../pages/parametros-calculo/ParametrosCalculoPage';
import { ProdutosPage } from '../pages/produtos/ProdutosPage';
import { RegrasAplicaveisPage } from '../pages/regras-tributarias/RegrasAplicaveisPage';
import { RegrasTributariasPage } from '../pages/regras-tributarias/RegrasTributariasPage';
import { UsuariosPage } from '../pages/usuarios/UsuariosPage';
import { ROTAS } from './rotas';

/**
 * Mapa de rotas. RequireAuth protege todas as páginas autenticadas; RequireAdmin, as áreas
 * administrativas. São proteções de interface: a autorização continua no backend.
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route path={ROTAS.login} element={<LoginPage />} />
      {/* Primeiro acesso: disponível somente enquanto o backend informa que não há usuários. */}
      <Route path={ROTAS.configuracaoInicial} element={<ConfiguracaoInicialPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<InicioPage />} />
          {/* Cotações e comparações: qualquer usuário autenticado (ADMIN ou USER), como no backend. */}
          <Route path={ROTAS.cotacoes} element={<CotacoesPage />} />
          <Route path={ROTAS.novaCotacao} element={<NovaCotacaoPage />} />
          <Route path={ROTAS.cotacaoDetalhe} element={<CotacaoDetalhePage />} />
          <Route path={ROTAS.comparacao} element={<ComparacaoPage />} />
          {/* Consulta para qualquer perfil; as ações de escrita aparecem somente ao ADMIN. */}
          <Route path={ROTAS.produtos} element={<ProdutosPage />} />
          <Route path={ROTAS.fornecedores} element={<FornecedoresPage />} />

          <Route element={<RequireAdmin />}>
            <Route path={ROTAS.nfe} element={<NfePage />} />
            <Route path={ROTAS.nfeDetalhe} element={<NfeDetalhePage />} />
            <Route path={ROTAS.regrasTributarias} element={<RegrasTributariasPage />} />
            <Route path={ROTAS.regrasAplicaveis} element={<RegrasAplicaveisPage />} />
            <Route path={ROTAS.parametrosCalculo} element={<ParametrosCalculoPage />} />
            <Route path={ROTAS.usuarios} element={<UsuariosPage />} />
          </Route>

          <Route path="*" element={<NaoEncontradaPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
