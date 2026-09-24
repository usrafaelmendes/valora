import { Alert, Anchor, Badge, Button, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { chaveComparacoesDaCotacao, chaveCotacao, cotacoesApi } from '../../api/cotacoes';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { CarregandoPagina } from '../../components/CarregandoPagina';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ListaDados } from '../../components/ListaDados';
import { MensagemErro } from '../../components/MensagemErro';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { NaoInformado, TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import { useAvisoNavegacao } from '../../hooks/useAvisoNavegacao';
import { caminhoComparacao, ROTAS } from '../../routes/rotas';
import type { Alternativa, Cotacao, OpcaoCotacao } from '../../types/cotacao';
import {
  formatarDataHora,
  formatarMoeda,
  formatarNumero,
  formatarPercentual,
} from '../../utils/formatacao';
import { AdicionarOpcao } from './AdicionarOpcao';
import { apresentacaoSituacao, formatarPosicao } from './apresentacaoComparacao';
import { NovaComparacao } from './NovaComparacao';
import { ROTULO_CAMPO_OPCAO } from './validacaoCotacao';

function VoltarParaCotacoes() {
  return (
    <Button component={Link} to={ROTAS.cotacoes} variant="default" size="xs">
      Voltar para cotações
    </Button>
  );
}

/** Detalhes de uma cotação (GET /cotacoes/{id}): opções, última comparação e histórico. */
export function CotacaoDetalhePage() {
  const { id } = useParams();
  const idNumerico = Number(id);
  const idValido = Number.isInteger(idNumerico) && idNumerico > 0;

  const consulta = useQuery({
    queryKey: chaveCotacao(idNumerico),
    queryFn: ({ signal }) => cotacoesApi.buscar(idNumerico, signal),
    enabled: idValido,
  });

  if (!idValido) {
    return (
      <>
        <CabecalhoPagina titulo="Cotação não encontrada" acoes={<VoltarParaCotacoes />} />
        <Text>O endereço acessado não corresponde a uma cotação.</Text>
      </>
    );
  }

  if (consulta.isPending) {
    return (
      <>
        <CabecalhoPagina titulo="Detalhes da cotação" acoes={<VoltarParaCotacoes />} />
        <CarregandoPagina mensagem="Carregando cotação…" />
      </>
    );
  }

  if (consulta.isError) {
    return (
      <>
        <CabecalhoPagina titulo="Detalhes da cotação" acoes={<VoltarParaCotacoes />} />
        <MensagemErro
          erro={consulta.error}
          titulo="Não foi possível carregar a cotação"
          aoTentarNovamente={() => {
            void consulta.refetch();
          }}
        />
      </>
    );
  }

  return <DetalhesCotacao cotacao={consulta.data} />;
}

function DetalhesCotacao({ cotacao }: { cotacao: Cotacao }) {
  const [avisoNavegacao, fecharAvisoNavegacao] = useAvisoNavegacao();
  const [adicionando, setAdicionando] = useState(false);
  const [incluida, setIncluida] = useState<OpcaoCotacao | null>(null);
  const ultima = cotacao.ultimaComparacao;

  // Situação de cada opção na última comparação, como gravada pelo backend.
  const resultados = new Map<number, Alternativa>(
    ultima
      ? [...ultima.alternativas, ...ultima.naoClassificadas].map((alternativa) => [
          alternativa.opcaoId,
          alternativa,
        ])
      : [],
  );

  const historico = useQuery({
    queryKey: chaveComparacoesDaCotacao(cotacao.id),
    queryFn: ({ signal }) => cotacoesApi.listarComparacoes(cotacao.id, signal),
  });

  const colunasOpcoes: ColunaTabela<OpcaoCotacao>[] = [
    {
      titulo: 'Opção',
      semQuebra: true,
      conteudo: (opcao) => `Nº ${String(opcao.id)}`,
    },
    {
      titulo: 'Fornecedor',
      conteudo: (opcao) => <Text size="sm">{opcao.fornecedorRazaoSocial}</Text>,
    },
    {
      titulo: 'Condição de pagamento',
      conteudo: (opcao) => opcao.condicaoPagamento ?? <NaoInformado />,
    },
    { titulo: 'Valores informados', conteudo: (opcao) => <ValoresInformados opcao={opcao} /> },
    { titulo: 'Dados fiscais', conteudo: (opcao) => <DadosFiscais opcao={opcao} /> },
    {
      titulo: 'Item de NF-e',
      semQuebra: true,
      conteudo: (opcao) =>
        opcao.nfeItemId === null ? <NaoInformado /> : `Nº ${String(opcao.nfeItemId)}`,
    },
    { titulo: 'Observação', conteudo: (opcao) => opcao.observacao ?? <NaoInformado /> },
    {
      titulo: 'Na última comparação',
      conteudo: (opcao) => <SituacaoNaUltima alternativa={resultados.get(opcao.id)} />,
    },
  ];

  return (
    <>
      <CabecalhoPagina
        titulo={`Cotação nº ${String(cotacao.id)}`}
        descricao={`${cotacao.produto.nome} · quantidade ${formatarNumero(cotacao.quantidade)}`}
        acoes={
          <>
            <VoltarParaCotacoes />
            <Button
              variant="default"
              onClick={() => {
                setIncluida(null);
                setAdicionando(true);
              }}
            >
              Adicionar opção
            </Button>
            <NovaComparacao cotacaoId={cotacao.id} />
          </>
        }
      />

      <Stack gap="lg">
        {avisoNavegacao && (
          <AvisoSucesso mensagem={avisoNavegacao} aoFechar={fecharAvisoNavegacao} />
        )}
        {incluida && (
          <AvisoSucesso
            mensagem={`Opção nº ${String(incluida.id)} (${incluida.fornecedorRazaoSocial}) incluída na cotação.`}
            aoFechar={() => {
              setIncluida(null);
            }}
          />
        )}
        {!cotacao.todasAsOpcoesComparadas && (
          <Alert color="yellow" variant="light" title="Nova comparação necessária">
            {ultima
              ? 'Há opções incluídas depois da última comparação: elas não fazem parte do resultado atual. Execute uma nova comparação para incluí-las.'
              : 'A cotação ainda não tem comparação. Execute uma comparação para ver o resultado.'}
          </Alert>
        )}

        <Paper withBorder p="lg" component="section" aria-labelledby="cotacao-dados">
          <Title id="cotacao-dados" order={2} size="h4" mb="md">
            Dados da cotação
          </Title>
          <ListaDados
            colunas={4}
            dados={[
              {
                rotulo: 'Produto',
                valor: (
                  <>
                    {cotacao.produto.nome}{' '}
                    {!cotacao.produto.ativo && (
                      <Badge size="xs" color="gray" variant="light">
                        Desativado
                      </Badge>
                    )}
                  </>
                ),
              },
              { rotulo: 'Quantidade', valor: formatarNumero(cotacao.quantidade) },
              { rotulo: 'Descrição', valor: cotacao.descricao },
              { rotulo: 'Criada em', valor: formatarDataHora(cotacao.criadoEm) },
            ]}
          />
        </Paper>

        <Paper withBorder p="lg" component="section" aria-labelledby="cotacao-ultima">
          <Group justify="space-between" mb="md" wrap="wrap">
            <Title id="cotacao-ultima" order={2} size="h4">
              Última comparação
            </Title>
            {ultima && (
              <Button component={Link} to={caminhoComparacao(ultima.id)} size="xs">
                Ver resultado completo
              </Button>
            )}
          </Group>
          {ultima ? (
            <ListaDados
              colunas={4}
              dados={[
                { rotulo: 'Comparação', valor: `Nº ${String(ultima.id)}` },
                { rotulo: 'Executada em', valor: formatarDataHora(ultima.executadoEm) },
                {
                  rotulo: 'Classificadas',
                  valor: `${String(ultima.totalClassificadas)} de ${String(ultima.totalOpcoes)}`,
                },
                {
                  rotulo: 'Na 1ª posição',
                  valor: primeiraPosicao(ultima.alternativas),
                },
              ]}
            />
          ) : (
            <Text size="sm" c="dimmed">
              Nenhuma comparação executada.
            </Text>
          )}
        </Paper>

        <section aria-labelledby="cotacao-opcoes">
          <Title id="cotacao-opcoes" order={2} size="h4" mb="sm">
            {`Opções (${String(cotacao.opcoes.length)})`}
          </Title>
          <TabelaDados
            rotulo="Opções da cotação"
            itens={cotacao.opcoes}
            colunas={colunasOpcoes}
            obterChave={(opcao) => opcao.id}
            larguraMinima={1100}
          />
        </section>

        <section aria-labelledby="cotacao-historico">
          <Title id="cotacao-historico" order={2} size="h4" mb="sm">
            Histórico de comparações
          </Title>
          <ResultadoConsulta
            consulta={historico}
            mensagemCarregando="Carregando histórico…"
            tituloErro="Não foi possível carregar o histórico de comparações"
            vazio={<EstadoVazio titulo="Nenhuma comparação executada" />}
          >
            {(comparacoes) => (
              <TabelaDados
                rotulo="Histórico de comparações"
                itens={comparacoes}
                obterChave={(comparacao) => comparacao.id}
                larguraMinima={560}
                colunas={[
                  {
                    titulo: 'Comparação',
                    semQuebra: true,
                    conteudo: (comparacao) => (
                      <Group gap="xs" wrap="nowrap">
                        <Anchor component={Link} to={caminhoComparacao(comparacao.id)} fw={500}>
                          {`Nº ${String(comparacao.id)}`}
                        </Anchor>
                        {comparacao.id === comparacoes[0]?.id && (
                          <Badge size="xs" variant="light">
                            Mais recente
                          </Badge>
                        )}
                      </Group>
                    ),
                  },
                  {
                    titulo: 'Executada em',
                    semQuebra: true,
                    conteudo: (comparacao) => formatarDataHora(comparacao.executadoEm),
                  },
                  {
                    titulo: 'Opções',
                    alinhamento: 'right',
                    conteudo: (comparacao) => String(comparacao.totalOpcoes),
                  },
                  {
                    titulo: 'Classificadas',
                    alinhamento: 'right',
                    conteudo: (comparacao) => String(comparacao.totalClassificadas),
                  },
                ]}
              />
            )}
          </ResultadoConsulta>
        </section>
      </Stack>

      {adicionando && (
        <AdicionarOpcao
          cotacao={cotacao}
          aoCancelar={() => {
            setAdicionando(false);
          }}
          aoAdicionar={(opcao) => {
            setAdicionando(false);
            setIncluida(opcao);
          }}
        />
      )}
    </>
  );
}

/** Fornecedor(es) na posição 1 do ranking gravado; nenhum quando não há classificadas. */
function primeiraPosicao(alternativas: Alternativa[]) {
  const primeiras = alternativas.filter((alternativa) => alternativa.posicao === 1);
  if (primeiras.length === 0) {
    return (
      <Text span c="dimmed" size="sm">
        Nenhuma opção classificada
      </Text>
    );
  }
  return primeiras
    .map(
      (alternativa) =>
        `${alternativa.fornecedor.razaoSocial}${
          alternativa.custoEfetivo === null ? '' : ` (${formatarMoeda(alternativa.custoEfetivo)})`
        }`,
    )
    .join(', ');
}

function SituacaoNaUltima({ alternativa }: { alternativa: Alternativa | undefined }) {
  if (!alternativa) {
    return (
      <Badge color="yellow" variant="light">
        Não comparada
      </Badge>
    );
  }
  const { rotulo, cor } = apresentacaoSituacao(alternativa.situacao);
  return (
    <Group gap={4} wrap="nowrap">
      {alternativa.posicao !== null && (
        <Badge color="teal" variant={alternativa.posicao === 1 ? 'filled' : 'light'}>
          {formatarPosicao(alternativa.posicao)}
        </Badge>
      )}
      <Badge color={cor} variant="light">
        {rotulo}
      </Badge>
    </Group>
  );
}

function ValoresInformados({ opcao }: { opcao: OpcaoCotacao }) {
  const { valores } = opcao;
  if (!valores) {
    return <NaoInformado />;
  }
  const linhas: [string, number | null][] = [
    [ROTULO_CAMPO_OPCAO['valores.valorProduto'], valores.valorProduto],
    [ROTULO_CAMPO_OPCAO['valores.valorIpi'], valores.valorIpi],
    [ROTULO_CAMPO_OPCAO['valores.valorFrete'], valores.valorFrete],
    [ROTULO_CAMPO_OPCAO['valores.valorSeguro'], valores.valorSeguro],
    [ROTULO_CAMPO_OPCAO['valores.valorOutrasDespesas'], valores.valorOutrasDespesas],
    [ROTULO_CAMPO_OPCAO['valores.valorDesconto'], valores.valorDesconto],
  ];
  return (
    <Stack gap={0}>
      {linhas
        .filter(([, valor]) => valor !== null)
        .map(([rotulo, valor]) => (
          <Text key={rotulo} size="xs" style={{ whiteSpace: 'nowrap' }}>
            {`${rotulo}: ${formatarMoeda(valor ?? 0)}`}
          </Text>
        ))}
    </Stack>
  );
}

function DadosFiscais({ opcao }: { opcao: OpcaoCotacao }) {
  const { dadosFiscais } = opcao;
  if (!dadosFiscais) {
    return <NaoInformado />;
  }
  const percentual = (valor: number | null) => (valor === null ? null : formatarPercentual(valor));
  const linhas: [string, string | null][] = [
    ['Origem', dadosFiscais.origemMercadoria],
    ['CFOP', dadosFiscais.cfop],
    ['ICMS', percentual(dadosFiscais.aliquotaIcms)],
    ['IPI', percentual(dadosFiscais.aliquotaIpi)],
    ['PIS', percentual(dadosFiscais.aliquotaPis)],
    ['COFINS', percentual(dadosFiscais.aliquotaCofins)],
  ];
  return (
    <Stack gap={0}>
      {linhas
        .filter(([, valor]) => valor !== null)
        .map(([rotulo, valor]) => (
          <Text key={rotulo} size="xs" style={{ whiteSpace: 'nowrap' }}>
            {`${rotulo}: ${valor ?? ''}`}
          </Text>
        ))}
    </Stack>
  );
}
