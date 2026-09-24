import { AppShell, Badge, Box, Burger, Button, Group, NavLink, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Link, Outlet, useLocation, matchPath } from 'react-router';
import { ROTULO_PERFIL } from '../auth/perfil';
import { useAuth } from '../auth/useAuth';
import { NOME_PRODUTO } from '../config/produto';
import { navegacaoDoPerfil } from '../routes/navegacao';
import { ROTAS } from '../routes/rotas';
import classes from './AppLayout.module.css';

function itemAtivo(caminho: string, atual: string): boolean {
  // "Início" só fica ativo na raiz; as demais áreas também nas subpáginas.
  // As comparações fazem parte da área de cotações.
  if (caminho === ROTAS.cotacoes && matchPath(ROTAS.comparacao, atual)) {
    return true;
  }
  return matchPath({ path: caminho, end: caminho === ROTAS.inicio }, atual) !== null;
}

/** Estrutura das páginas autenticadas: cabeçalho com usuário e logout, menu por perfil e conteúdo. */
export function AppLayout() {
  const { usuario, logout } = useAuth();
  const { pathname } = useLocation();
  const [menuAberto, { toggle: alternarMenu, close: fecharMenu }] = useDisclosure(false);

  if (!usuario) {
    return null;
  }

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 248, breakpoint: 'sm', collapsed: { mobile: !menuAberto } }}
      padding="lg"
    >
      <a href="#conteudo" className="cp-pular-conteudo">
        Pular para o conteúdo
      </a>

      <AppShell.Header className={classes.cabecalho}>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={menuAberto}
              onClick={alternarMenu}
              hiddenFrom="sm"
              size="sm"
              color="white"
              aria-label={menuAberto ? 'Fechar menu' : 'Abrir menu'}
            />
            <Link to={ROTAS.inicio} className={classes.marca}>
              {NOME_PRODUTO}
            </Link>
          </Group>

          <Group gap="sm" wrap="nowrap">
            <Box visibleFrom="xs" ta="right">
              <Text size="sm" fw={500} lh={1.2}>
                {usuario.nome}
              </Text>
            </Box>
            <Badge variant="white" color="petroleo" radius="sm" aria-label="Perfil de acesso">
              {ROTULO_PERFIL[usuario.perfil]}
            </Badge>
            <Button variant="white" color="dark" size="xs" onClick={logout}>
              Sair
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs" aria-label="Menu principal" component="nav">
        {navegacaoDoPerfil(usuario.perfil).map((secao) => (
          <Box key={secao.titulo} mb="sm">
            <Text className={classes.tituloSecao} component="h2">
              {secao.titulo}
            </Text>
            {secao.itens.map((item) => {
              const ativo = itemAtivo(item.caminho, pathname);
              return (
                <NavLink
                  key={item.caminho}
                  component={Link}
                  to={item.caminho}
                  label={item.rotulo}
                  active={ativo}
                  aria-current={ativo ? 'page' : undefined}
                  onClick={fecharMenu}
                />
              );
            })}
          </Box>
        ))}
      </AppShell.Navbar>

      <AppShell.Main id="conteudo" tabIndex={-1} className={classes.principal}>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
