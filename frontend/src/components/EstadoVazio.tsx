import { EmptyState, Paper } from '@mantine/core';
import type { ReactNode } from 'react';

interface EstadoVazioProps {
  titulo: string;
  descricao?: ReactNode;
  /** Ação sugerida (ex.: botão de cadastro, somente para quem pode cadastrar). */
  acao?: ReactNode;
}

/** Listagem sem registros: explica a situação e, quando cabe, sugere o próximo passo. */
export function EstadoVazio({ titulo, descricao, acao }: EstadoVazioProps) {
  return (
    <Paper withBorder p="xl" role="status">
      <EmptyState title={titulo} description={descricao} size="sm">
        {acao && <EmptyState.Actions>{acao}</EmptyState.Actions>}
      </EmptyState>
    </Paper>
  );
}
