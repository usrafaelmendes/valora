import { Table, Text } from '@mantine/core';
import type { ReactNode } from 'react';

export interface ColunaTabela<T> {
  titulo: string;
  conteudo: (item: T) => ReactNode;
  alinhamento?: 'left' | 'right' | 'center';
  /** Evita quebra de linha (ex.: CNPJ, códigos, botões de ação). */
  semQuebra?: boolean;
}

interface TabelaDadosProps<T> {
  /** Nome acessível da tabela (lido por leitores de tela). */
  rotulo: string;
  itens: T[];
  colunas: ColunaTabela<T>[];
  obterChave: (item: T) => string | number;
  /** Largura mínima antes de habilitar a rolagem horizontal em telas estreitas. */
  larguraMinima?: number;
}

/** Tabela de listagem com rolagem horizontal em telas estreitas. */
export function TabelaDados<T>({
  rotulo,
  itens,
  colunas,
  obterChave,
  larguraMinima = 720,
}: TabelaDadosProps<T>) {
  return (
    <Table.ScrollContainer minWidth={larguraMinima}>
      <Table aria-label={rotulo} withTableBorder striped>
        <Table.Thead>
          <Table.Tr>
            {colunas.map((coluna) => (
              <Table.Th key={coluna.titulo} ta={coluna.alinhamento}>
                {coluna.titulo}
              </Table.Th>
            ))}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {itens.map((item) => (
            <Table.Tr key={obterChave(item)}>
              {colunas.map((coluna) => (
                <Table.Td
                  key={coluna.titulo}
                  ta={coluna.alinhamento}
                  style={coluna.semQuebra ? { whiteSpace: 'nowrap' } : undefined}
                >
                  {coluna.conteudo(item)}
                </Table.Td>
              ))}
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

/** Valor opcional ausente, exibido de forma discreta e uniforme nas tabelas. */
export function NaoInformado() {
  return (
    <Text span c="dimmed" size="sm">
      Não informado
    </Text>
  );
}
