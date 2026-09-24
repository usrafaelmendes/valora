import { Center, Loader, Stack, Text } from '@mantine/core';

/** Indicador de carregamento de página inteira ou de uma área. */
export function CarregandoPagina({ mensagem = 'Carregando…' }: { mensagem?: string }) {
  return (
    <Center mih={240} role="status" aria-live="polite">
      <Stack align="center" gap="xs">
        <Loader size="md" aria-hidden />
        <Text c="dimmed" size="sm">
          {mensagem}
        </Text>
      </Stack>
    </Center>
  );
}
