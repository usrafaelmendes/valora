import { Alert, Anchor, Button, Paper, Stack, Text } from '@mantine/core';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router';
import { chaveComparacao, comparacoesApi } from '../../api/comparacoes';
import { chaveComparacoesDaCotacao, cotacoesApi } from '../../api/cotacoes';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { CarregandoPagina } from '../../components/CarregandoPagina';
import { ListaDados } from '../../components/ListaDados';
import { MensagemErro } from '../../components/MensagemErro';
import { useAvisoNavegacao } from '../../hooks/useAvisoNavegacao';
import { caminhoComparacao, caminhoCotacao, ROTAS } from '../../routes/rotas';
import type { Comparacao } from '../../types/cotacao';
import { formatarDataHora, formatarNumero } from '../../utils/formatacao';
import { salvarArquivo } from '../../utils/salvarArquivo';
import { NovaComparacao } from './NovaComparacao';
import { ResultadoComparacao } from './ResultadoComparacao';

function VoltarParaCotacoes() {
  return (
    <Button component={Link} to={ROTAS.cotacoes} variant="default" size="xs">
      Voltar para cotações
    </Button>
  );
}

/** Comparação gravada (GET /comparacoes/{id}): resultado, rastreabilidade e download. */
export function ComparacaoPage() {
  const { id } = useParams();
  const idNumerico = Number(id);
  const idValido = Number.isInteger(idNumerico) && idNumerico > 0;

  const consulta = useQuery({
    queryKey: chaveComparacao(idNumerico),
    queryFn: ({ signal }) => comparacoesApi.buscar(idNumerico, signal),
    enabled: idValido,
  });

  if (!idValido) {
    return (
      <>
        <CabecalhoPagina titulo="Comparação não encontrada" acoes={<VoltarParaCotacoes />} />
        <Text>O endereço acessado não corresponde a uma comparação.</Text>
      </>
    );
  }

  if (consulta.isPending) {
    return (
      <>
        <CabecalhoPagina titulo="Comparação" acoes={<VoltarParaCotacoes />} />
        <CarregandoPagina mensagem="Carregando comparação…" />
      </>
    );
  }

  if (consulta.isError) {
    return (
      <>
        <CabecalhoPagina titulo="Comparação" acoes={<VoltarParaCotacoes />} />
        <MensagemErro
          erro={consulta.error}
          titulo="Não foi possível carregar a comparação"
          aoTentarNovamente={() => {
            void consulta.refetch();
          }}
        />
      </>
    );
  }

  return <DetalhesComparacao comparacao={consulta.data} />;
}

function DetalhesComparacao({ comparacao }: { comparacao: Comparacao }) {
  const [aviso, fecharAviso] = useAvisoNavegacao();
  const historico = useQuery({
    queryKey: chaveComparacoesDaCotacao(comparacao.cotacaoId),
    queryFn: ({ signal }) => cotacoesApi.listarComparacoes(comparacao.cotacaoId, signal),
  });
  // O histórico vem do backend da mais recente para a mais antiga.
  const maisRecente = historico.data?.[0];

  const baixar = useMutation({
    mutationFn: () => comparacoesApi.download(comparacao.id),
    onSuccess: (arquivo) => {
      salvarArquivo(arquivo, `comparacao-${String(comparacao.id)}.csv`);
    },
  });

  return (
    <>
      <CabecalhoPagina
        titulo={`Comparação nº ${String(comparacao.id)}`}
        descricao={
          <>
            {'Cotação '}
            <Anchor component={Link} to={caminhoCotacao(comparacao.cotacaoId)} size="sm">
              {`nº ${String(comparacao.cotacaoId)}`}
            </Anchor>
            {` · ${comparacao.produtoNome}. Resultado gravado pelo backend: os valores não são recalculados nesta tela.`}
          </>
        }
        acoes={
          <>
            <Button
              variant="default"
              loading={baixar.isPending}
              onClick={() => {
                baixar.mutate();
              }}
            >
              Baixar tabela (CSV)
            </Button>
            <Button component={Link} to={caminhoCotacao(comparacao.cotacaoId)} variant="default">
              Ver cotação
            </Button>
            <NovaComparacao cotacaoId={comparacao.cotacaoId} />
          </>
        }
      />

      <Stack gap="lg">
        {aviso && <AvisoSucesso mensagem={aviso} aoFechar={fecharAviso} />}
        {baixar.isError && (
          <MensagemErro erro={baixar.error} titulo="Não foi possível baixar a tabela" />
        )}
        {maisRecente && maisRecente.id !== comparacao.id && (
          <Alert color="blue" variant="light" title="Comparação anterior">
            {'Existe uma comparação mais recente desta cotação: '}
            <Anchor component={Link} to={caminhoComparacao(maisRecente.id)} size="sm">
              {`comparação nº ${String(maisRecente.id)}`}
            </Anchor>
            {'. Este resultado continua disponível como histórico, sem alteração.'}
          </Alert>
        )}

        <Paper withBorder p="lg" component="section" aria-label="Resumo da comparação">
          <ListaDados
            colunas={4}
            dados={[
              { rotulo: 'Produto', valor: comparacao.produtoNome },
              { rotulo: 'Quantidade', valor: formatarNumero(comparacao.quantidade) },
              { rotulo: 'Executada em', valor: formatarDataHora(comparacao.executadoEm) },
              { rotulo: 'Opções', valor: String(comparacao.totalOpcoes) },
              { rotulo: 'Classificadas', valor: String(comparacao.totalClassificadas) },
              {
                rotulo: 'Não classificadas',
                valor: String(comparacao.naoClassificadas.length),
              },
              { rotulo: 'Critério de ordenação', valor: comparacao.criterioOrdenacao },
            ]}
          />
        </Paper>

        <ResultadoComparacao comparacao={comparacao} />
      </Stack>
    </>
  );
}
