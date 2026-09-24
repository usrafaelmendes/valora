import { SimpleGrid, Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { NaoInformado } from './TabelaDados';

export interface DadoRotulado {
  rotulo: string;
  /** null ou undefined é exibido como "Não informado". */
  valor: ReactNode;
}

/** Pares rótulo/valor em grade responsiva (ex.: dados de identificação de um documento). */
export function ListaDados({ dados, colunas = 3 }: { dados: DadoRotulado[]; colunas?: number }) {
  return (
    <SimpleGrid component="dl" cols={{ base: 1, xs: 2, md: colunas }} spacing="md" m={0}>
      {dados.map(({ rotulo, valor }) => (
        <Stack key={rotulo} gap={2}>
          <Text component="dt" size="xs" c="dimmed" fw={500}>
            {rotulo}
          </Text>
          <Text component="dd" size="sm" m={0} style={{ overflowWrap: 'anywhere' }}>
            {valor ?? <NaoInformado />}
          </Text>
        </Stack>
      ))}
    </SimpleGrid>
  );
}
