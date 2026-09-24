import { Alert, Badge, Button, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { CHAVE_PARAMETROS_CALCULO, parametrosCalculoApi } from '../../api/parametrosCalculo';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import type { ParametroCalculo } from '../../types/parametroCalculo';
import { formatarDataHora } from '../../utils/formatacao';
import { descreverValor, tituloDoParametro } from './apresentacaoParametro';
import { ParametroFormulario } from './ParametroFormulario';

/**
 * Parâmetros gerais do cálculo (docs/REGRAS_TRIBUTARIAS.md §11). As chaves são
 * criadas pelo backend; o ADMIN só define os valores. Parâmetro não definido nunca recebe
 * um valor presumido pelo frontend.
 */
export function ParametrosCalculoPage() {
  const [emEdicao, setEmEdicao] = useState<ParametroCalculo | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: CHAVE_PARAMETROS_CALCULO,
    queryFn: ({ signal }) => parametrosCalculoApi.listar(signal),
  });

  const naoDefinidos = consulta.data?.filter((parametro) => !parametro.definido) ?? [];

  const colunas: ColunaTabela<ParametroCalculo>[] = [
    {
      titulo: 'Parâmetro',
      conteudo: (parametro) => (
        <Stack gap={2} maw={380}>
          <Text fw={500} size="sm">
            {tituloDoParametro(parametro.chave)}
          </Text>
          <Text size="xs" c="dimmed">
            {parametro.descricao}
          </Text>
          <Text size="xs" c="dimmed" ff="monospace">
            {parametro.chave}
          </Text>
        </Stack>
      ),
    },
    {
      titulo: 'Valor atual',
      conteudo: (parametro) =>
        parametro.definido && parametro.valor !== null ? (
          <Stack gap={2}>
            <Text size="sm">{descreverValor(parametro.valor)}</Text>
            <Text size="xs" c="dimmed" ff="monospace" style={{ overflowWrap: 'anywhere' }}>
              {parametro.valor}
            </Text>
          </Stack>
        ) : (
          <Badge color="yellow" variant="light">
            Não definido
          </Badge>
        ),
    },
    {
      titulo: 'Versão',
      semQuebra: true,
      conteudo: (parametro) => (
        <Stack gap={0}>
          <Text size="sm">{`v${String(parametro.versao)}`}</Text>
          <Text size="xs" c="dimmed">
            {formatarDataHora(parametro.atualizadoEm)}
          </Text>
        </Stack>
      ),
    },
    {
      titulo: 'Ações',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (parametro) => (
        <Button
          size="xs"
          variant="default"
          aria-label={`Alterar ${tituloDoParametro(parametro.chave)}`}
          onClick={() => {
            setAviso(null);
            setEmEdicao(parametro);
          }}
        >
          Alterar
        </Button>
      ),
    },
  ];

  return (
    <>
      <CabecalhoPagina
        titulo="Parâmetros de cálculo"
        descricao="Configurações gerais usadas por todos os cálculos: fontes dos dados, composição dos valores e arredondamento. Os valores são definidos pelo administrador."
      />

      <Stack gap="md">
        {aviso && (
          <AvisoSucesso
            mensagem={aviso}
            aoFechar={() => {
              setAviso(null);
            }}
          />
        )}

        {naoDefinidos.length > 0 && (
          <Alert color="yellow" variant="light" title="Há parâmetros não definidos">
            {naoDefinidos.length === 1
              ? '1 parâmetro ainda não foi definido'
              : `${String(naoDefinidos.length)} parâmetros ainda não foram definidos`}
            : {naoDefinidos.map((parametro) => tituloDoParametro(parametro.chave)).join(', ')}.
            Enquanto um parâmetro necessário não estiver definido, o cálculo fica incompleto e
            informa o que falta. O sistema não escolhe esses valores sozinho.
          </Alert>
        )}

        <ResultadoConsulta
          consulta={consulta}
          mensagemCarregando="Carregando parâmetros…"
          tituloErro="Não foi possível carregar os parâmetros de cálculo"
          vazio={
            <EstadoVazio
              titulo="Nenhum parâmetro encontrado"
              descricao="Os parâmetros são criados pelas migrations do backend."
            />
          }
        >
          {(parametros) => (
            <TabelaDados
              rotulo="Parâmetros de cálculo"
              itens={parametros}
              colunas={colunas}
              obterChave={(parametro) => parametro.chave}
            />
          )}
        </ResultadoConsulta>
      </Stack>

      {emEdicao && (
        <ParametroFormulario
          parametro={emEdicao}
          aoCancelar={() => {
            setEmEdicao(null);
          }}
          aoSalvar={(salvo) => {
            setAviso(
              salvo.definido
                ? `Parâmetro "${tituloDoParametro(salvo.chave)}" atualizado.`
                : `Parâmetro "${tituloDoParametro(salvo.chave)}" voltou a "não definido".`,
            );
            setEmEdicao(null);
          }}
        />
      )}
    </>
  );
}
