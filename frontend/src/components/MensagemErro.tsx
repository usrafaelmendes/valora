import { Alert, Button, Group, List } from '@mantine/core';
import { ApiError, mensagemDeErro } from '../api/ApiError';

interface MensagemErroProps {
  erro: unknown;
  titulo?: string;
  /** Quando informado, exibe o botão "Tentar novamente". */
  aoTentarNovamente?: () => void;
  /** Campos cujos erros já aparecem no próprio formulário e não precisam ser listados. */
  camposOcultos?: string[];
}

/** Exibe um erro da API de forma padronizada, incluindo erros de validação por campo. */
export function MensagemErro({
  erro,
  titulo = 'Não foi possível concluir a operação',
  aoTentarNovamente,
  camposOcultos = [],
}: MensagemErroProps) {
  const erros =
    erro instanceof ApiError
      ? Object.entries(erro.erros).filter(([campo]) => !camposOcultos.includes(campo))
      : [];
  return (
    <Alert color="red" variant="light" title={titulo} role="alert">
      {mensagemDeErro(erro)}
      {erros.length > 0 && (
        <List size="sm" mt="xs">
          {erros.map(([campo, mensagem]) => (
            <List.Item key={campo}>{mensagem}</List.Item>
          ))}
        </List>
      )}
      {aoTentarNovamente && (
        <Group mt="sm">
          <Button size="xs" variant="default" onClick={aoTentarNovamente}>
            Tentar novamente
          </Button>
        </Group>
      )}
    </Alert>
  );
}
