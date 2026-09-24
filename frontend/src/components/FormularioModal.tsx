import { Button, Group, Modal, Stack } from '@mantine/core';
import type { ReactNode, SubmitEvent } from 'react';
import { MensagemErro } from './MensagemErro';

interface FormularioModalProps {
  titulo: string;
  children: ReactNode;
  rotuloSalvar: string;
  salvando: boolean;
  /** Erro da última tentativa de salvar. */
  erro?: unknown;
  /** Campos exibidos no formulário: seus erros aparecem no próprio campo, não na lista. */
  campos: string[];
  aoEnviar: () => void;
  aoCancelar: () => void;
}

/**
 * Modal de cadastro/edição: título, campos, erro da API e botões "Cancelar"/"Salvar".
 * Renderize somente quando aberto, para que o formulário comece com os dados atuais.
 */
export function FormularioModal({
  titulo,
  children,
  rotuloSalvar,
  salvando,
  erro,
  campos,
  aoEnviar,
  aoCancelar,
}: FormularioModalProps) {
  const enviar = (evento: SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault();
    aoEnviar();
  };

  return (
    <Modal
      opened
      onClose={() => {
        if (!salvando) {
          aoCancelar();
        }
      }}
      title={titulo}
      size="lg"
      centered
    >
      <form onSubmit={enviar} noValidate>
        <Stack gap="md">
          {erro ? <MensagemErro erro={erro} camposOcultos={campos} /> : null}
          {children}
          <Group justify="flex-end" gap="sm" mt="xs">
            <Button variant="default" onClick={aoCancelar} disabled={salvando}>
              Cancelar
            </Button>
            <Button type="submit" loading={salvando}>
              {rotuloSalvar}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
