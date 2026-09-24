import { Button, Text } from '@mantine/core';
import { Link } from 'react-router';
import { CabecalhoPagina } from '../components/CabecalhoPagina';
import { ROTAS } from '../routes/rotas';

export function NaoEncontradaPage() {
  return (
    <>
      <CabecalhoPagina titulo="Página não encontrada" />
      <Text mb="md">O endereço acessado não existe. Confira o link ou use o menu.</Text>
      <Button component={Link} to={ROTAS.inicio} variant="default">
        Voltar ao início
      </Button>
    </>
  );
}
