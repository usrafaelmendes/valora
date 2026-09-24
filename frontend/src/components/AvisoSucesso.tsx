import { Alert } from '@mantine/core';
import type { ReactNode } from 'react';

/** Confirmação de operação concluída, exibida no topo da página até ser fechada. */
export function AvisoSucesso({
  mensagem,
  aoFechar,
}: {
  mensagem: ReactNode;
  aoFechar: () => void;
}) {
  return (
    <Alert
      color="teal"
      variant="light"
      role="status"
      withCloseButton
      closeButtonLabel="Fechar aviso"
      onClose={aoFechar}
    >
      {mensagem}
    </Alert>
  );
}
