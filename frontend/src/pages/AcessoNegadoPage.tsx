import { Button, Text } from '@mantine/core';
import { Link } from 'react-router';
import { CabecalhoPagina } from '../components/CabecalhoPagina';
import { ROTAS } from '../routes/rotas';

export function AcessoNegadoPage() {
  return (
    <>
      <CabecalhoPagina titulo="Acesso restrito" />
      <Text mb="md">
        Esta área é exclusiva de administradores. Se precisar dela, peça acesso a um administrador
        do sistema.
      </Text>
      <Button component={Link} to={ROTAS.inicio} variant="default">
        Voltar ao início
      </Button>
    </>
  );
}
