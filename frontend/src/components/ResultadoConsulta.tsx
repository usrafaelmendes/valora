import { Stack } from '@mantine/core';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { CarregandoPagina } from './CarregandoPagina';
import { MensagemErro } from './MensagemErro';

interface ResultadoConsultaProps<T> {
  consulta: UseQueryResult<T[]>;
  mensagemCarregando: string;
  tituloErro: string;
  /** Exibido quando a consulta retorna uma lista vazia. */
  vazio: ReactNode;
  children: (itens: T[]) => ReactNode;
}

/**
 * Estados padronizados de uma listagem: carregando, erro (com "Tentar novamente"),
 * vazia e com dados. Se uma nova busca falhar, os dados anteriores continuam visíveis.
 */
export function ResultadoConsulta<T>({
  consulta,
  mensagemCarregando,
  tituloErro,
  vazio,
  children,
}: ResultadoConsultaProps<T>) {
  if (consulta.isPending) {
    return <CarregandoPagina mensagem={mensagemCarregando} />;
  }

  const erro = consulta.isError && (
    <MensagemErro
      erro={consulta.error}
      titulo={tituloErro}
      aoTentarNovamente={() => {
        void consulta.refetch();
      }}
    />
  );
  if (!consulta.data) {
    return erro;
  }

  return (
    <Stack gap="md">
      {erro}
      {consulta.data.length === 0 ? vazio : children(consulta.data)}
    </Stack>
  );
}
