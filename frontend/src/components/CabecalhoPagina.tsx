import { Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import { useDocumentTitle } from '../hooks/useDocumentTitle';

interface CabecalhoPaginaProps {
  titulo: string;
  descricao?: ReactNode;
  /** Ações da página (ex.: botão "Cadastrar fornecedor"). */
  acoes?: ReactNode;
}

/** Título da página (também aplicado à aba do navegador), descrição e ações. */
export function CabecalhoPagina({ titulo, descricao, acoes }: CabecalhoPaginaProps) {
  useDocumentTitle(titulo);
  return (
    <Group justify="space-between" align="flex-start" mb="lg" wrap="wrap">
      <Stack gap={4}>
        <Title order={1} size="h2">
          {titulo}
        </Title>
        {descricao && (
          <Text c="dimmed" size="sm" maw={720}>
            {descricao}
          </Text>
        )}
      </Stack>
      {acoes && <Group gap="sm">{acoes}</Group>}
    </Group>
  );
}
