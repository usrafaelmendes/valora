import { Anchor, Paper, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';
import { useAuth } from '../auth/useAuth';
import { CabecalhoPagina } from '../components/CabecalhoPagina';
import { navegacaoDoPerfil } from '../routes/navegacao';
import { ROTAS } from '../routes/rotas';

export function InicioPage() {
  const { usuario } = useAuth();
  if (!usuario) {
    return null;
  }
  const secoes = navegacaoDoPerfil(usuario.perfil);

  return (
    <>
      <CabecalhoPagina
        titulo={`Olá, ${usuario.nome}`}
        descricao="Escolha uma área para começar. As opções exibidas dependem do seu perfil de acesso."
      />
      <SimpleGrid cols={{ base: 1, sm: secoes.length }} spacing="lg" maw={880}>
        {secoes.map((secao) => (
          <Paper key={secao.titulo} withBorder p="lg" component="section">
            <Stack gap="xs">
              <Title order={2} size="h4">
                {secao.titulo}
              </Title>
              {secao.itens
                .filter((item) => item.caminho !== ROTAS.inicio)
                .map((item) => (
                  <Anchor key={item.caminho} component={Link} to={item.caminho}>
                    {item.rotulo}
                  </Anchor>
                ))}
            </Stack>
          </Paper>
        ))}
      </SimpleGrid>
      <Text c="dimmed" size="xs" mt="xl">
        Os valores e cálculos apresentados pelo sistema vêm do servidor, conforme as regras
        configuradas pelo administrador.
      </Text>
    </>
  );
}
