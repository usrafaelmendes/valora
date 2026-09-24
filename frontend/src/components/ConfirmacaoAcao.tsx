import { Button, Group, Modal, Stack } from '@mantine/core';
import type { ReactNode } from 'react';
import { MensagemErro } from './MensagemErro';

interface ConfirmacaoAcaoProps {
  titulo: string;
  children: ReactNode;
  rotuloConfirmar: string;
  /** Cor do botão de confirmação: vermelho (padrão) para ações destrutivas. */
  corConfirmar?: string;
  /** Operação em andamento: desabilita o cancelamento e mostra carregamento. */
  processando: boolean;
  erro?: unknown;
  aoConfirmar: () => void;
  aoCancelar: () => void;
}

/**
 * Confirmação de ação (ex.: desativar um cadastro, executar nova comparação).
 * Renderize somente quando aberta.
 */
export function ConfirmacaoAcao({
  titulo,
  children,
  rotuloConfirmar,
  corConfirmar = 'red',
  processando,
  erro,
  aoConfirmar,
  aoCancelar,
}: ConfirmacaoAcaoProps) {
  return (
    <Modal
      opened
      onClose={() => {
        if (!processando) {
          aoCancelar();
        }
      }}
      title={titulo}
      centered
    >
      <Stack gap="md">
        <div>{children}</div>
        {erro ? <MensagemErro erro={erro} /> : null}
        <Group justify="flex-end" gap="sm">
          <Button variant="default" onClick={aoCancelar} disabled={processando}>
            Cancelar
          </Button>
          <Button color={corConfirmar} onClick={aoConfirmar} loading={processando}>
            {rotuloConfirmar}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
