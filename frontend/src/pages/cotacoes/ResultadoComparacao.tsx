import { Accordion, Alert, Badge, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ListaDados } from '../../components/ListaDados';
import { NaoInformado, TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import { ROTULO_TIPO_FORNECEDOR } from '../../types/fornecedor';
import type { Alternativa, Comparacao } from '../../types/cotacao';
import { formatarMoeda } from '../../utils/formatacao';
import { descreverValor, tituloDoParametro } from '../parametros-calculo/apresentacaoParametro';
import {
  apresentacaoSituacao,
  apresentacaoSituacaoCredito,
  formatarPosicao,
  rotuloStatusCalculo,
  rotuloTributo,
} from './apresentacaoComparacao';
import { DetalhesCalculo } from './DetalhesCalculo';

/**
 * Resultado oficial de uma comparação, exatamente como gravado pelo backend:
 * - alternativas classificadas na ordem recebida (posição e empate do backend);
 * - opções não classificadas em seção separada, com situação e motivo, sem posição;
 * - detalhes de cada cálculo e a configuração (parâmetros e regras com versão) usada.
 * O frontend não calcula, não reordena e não decide a classificação.
 */
export function ResultadoComparacao({ comparacao }: { comparacao: Comparacao }) {
  const { alternativas, naoClassificadas } = comparacao;
  // Os detalhes de cada cálculo só são montados quando o painel é aberto.
  const [abertos, setAbertos] = useState<string[]>([]);
  const primeiras = alternativas.filter((alternativa) => alternativa.posicao === 1);

  return (
    <Stack gap="xl">
      {primeiras.map((alternativa) => (
        <DestaquePrimeiraPosicao key={alternativa.opcaoId} alternativa={alternativa} />
      ))}

      <section aria-labelledby="comparacao-classificadas">
        <Title id="comparacao-classificadas" order={2} size="h4" mb={4}>
          {`Alternativas classificadas (${String(alternativas.length)})`}
        </Title>
        <Text size="sm" c="dimmed" mb="sm">
          {`Ordem registrada pelo backend. Critério: ${comparacao.criterioOrdenacao}`}
        </Text>
        {alternativas.length === 0 ? (
          <EstadoVazio
            titulo="Nenhuma opção classificada"
            descricao="Nenhuma opção tem custo efetivo válido e participação confirmada. Veja os motivos nas opções não classificadas."
          />
        ) : (
          <TabelaDados
            rotulo="Alternativas classificadas"
            itens={alternativas}
            colunas={COLUNAS_CLASSIFICADAS}
            obterChave={(alternativa) => alternativa.opcaoId}
            larguraMinima={980}
          />
        )}
      </section>

      <section aria-labelledby="comparacao-nao-classificadas">
        <Title id="comparacao-nao-classificadas" order={2} size="h4" mb={4}>
          {`Não classificadas (${String(naoClassificadas.length)})`}
        </Title>
        <Text size="sm" c="dimmed" mb="sm">
          Opções fora do ranking: não têm posição e não são tratadas como custo zero.
        </Text>
        {naoClassificadas.length === 0 ? (
          <Text size="sm">Todas as opções foram classificadas.</Text>
        ) : (
          <TabelaDados
            rotulo="Opções não classificadas"
            itens={naoClassificadas}
            colunas={COLUNAS_NAO_CLASSIFICADAS}
            obterChave={(alternativa) => alternativa.opcaoId}
            larguraMinima={980}
          />
        )}
      </section>

      <section aria-labelledby="comparacao-detalhes">
        <Title id="comparacao-detalhes" order={2} size="h4" mb={4}>
          Detalhes do cálculo por opção
        </Title>
        <Text size="sm" c="dimmed" mb="sm">
          Valores, alíquotas, regras com versão, créditos individuais e pendências registrados em
          cada cálculo.
        </Text>
        <Accordion
          multiple
          variant="separated"
          chevronPosition="left"
          value={abertos}
          onChange={setAbertos}
        >
          {[...alternativas, ...naoClassificadas].map((alternativa) => (
            <Accordion.Item key={alternativa.opcaoId} value={String(alternativa.opcaoId)}>
              <Accordion.Control
                aria-label={`Detalhes do cálculo: ${identificacao(alternativa)} — ${alternativa.fornecedor.razaoSocial}`}
              >
                <Group gap="xs" wrap="wrap">
                  <Text size="sm" fw={500}>
                    {`${identificacao(alternativa)} — ${alternativa.fornecedor.razaoSocial}`}
                  </Text>
                  <SituacaoBadge alternativa={alternativa} />
                </Group>
              </Accordion.Control>
              <Accordion.Panel>
                {!abertos.includes(String(alternativa.opcaoId)) ? null : alternativa.calculo ? (
                  <DetalhesCalculo calculo={alternativa.calculo} />
                ) : (
                  <Text size="sm">
                    A opção não foi calculada nesta comparação. Motivo registrado:{' '}
                    {alternativa.motivo ?? <NaoInformado />}
                  </Text>
                )}
              </Accordion.Panel>
            </Accordion.Item>
          ))}
        </Accordion>
      </section>

      <ConfiguracaoComparacao comparacao={comparacao} />
    </Stack>
  );
}

function identificacao(alternativa: Alternativa): string {
  return alternativa.posicao === null
    ? 'Não classificada'
    : `${formatarPosicao(alternativa.posicao)} lugar`;
}

function SituacaoBadge({ alternativa }: { alternativa: Alternativa }) {
  const { rotulo, cor } = apresentacaoSituacao(alternativa.situacao);
  return (
    <Badge variant="light" color={cor}>
      {rotulo}
    </Badge>
  );
}

/** Opção na posição 1 do ranking gravado, com destaque para o custo efetivo. */
function DestaquePrimeiraPosicao({ alternativa }: { alternativa: Alternativa }) {
  return (
    <Paper
      withBorder
      p="lg"
      component="section"
      aria-label="Primeira posição"
      style={{ borderColor: 'var(--mantine-color-teal-6)', borderWidth: 2 }}
    >
      <Group justify="space-between" align="flex-start" wrap="wrap" gap="md">
        <Stack gap={4}>
          <Group gap="xs">
            <Badge color="teal" size="lg">
              1º lugar
            </Badge>
            {alternativa.empate && (
              <Badge color="yellow" variant="light" size="lg">
                Empate
              </Badge>
            )}
          </Group>
          <Text size="lg" fw={600}>
            {alternativa.fornecedor.razaoSocial}
          </Text>
          <Text size="sm" c="dimmed">
            {[
              alternativa.fornecedor.tipo && ROTULO_TIPO_FORNECEDOR[alternativa.fornecedor.tipo],
              `Condição de pagamento: ${alternativa.condicaoPagamento ?? 'não informada'}`,
            ]
              .filter(Boolean)
              .join(' · ')}
          </Text>
        </Stack>
        <Stack gap={0} align="flex-end">
          <Text size="xs" c="dimmed">
            Custo efetivo
          </Text>
          <Text size="xl" fw={700} c="teal.8">
            {alternativa.custoEfetivo === null ? '—' : formatarMoeda(alternativa.custoEfetivo)}
          </Text>
          <Text size="xs" c="dimmed">
            {`Valor da operação ${alternativa.valorOperacao === null ? '—' : formatarMoeda(alternativa.valorOperacao)} − créditos ${alternativa.totalCreditos === null ? '—' : formatarMoeda(alternativa.totalCreditos)}`}
          </Text>
        </Stack>
      </Group>
      {alternativa.empate && (
        <Text size="sm" mt="sm">
          Esta opção tem o mesmo custo efetivo de outra opção classificada. A ordem entre elas segue
          o critério registrado na comparação.
        </Text>
      )}
    </Paper>
  );
}

function Fornecedor({ alternativa }: { alternativa: Alternativa }) {
  const { fornecedor } = alternativa;
  return (
    <>
      <Text size="sm" fw={500}>
        {fornecedor.razaoSocial}
      </Text>
      <Group gap={4}>
        {fornecedor.tipo && (
          <Text size="xs" c="dimmed">
            {ROTULO_TIPO_FORNECEDOR[fornecedor.tipo]}
          </Text>
        )}
        {!fornecedor.ativo && (
          <Badge size="xs" color="gray" variant="light">
            Desativado
          </Badge>
        )}
      </Group>
    </>
  );
}

function CondicaoPagamento({ alternativa }: { alternativa: Alternativa }) {
  return (
    <>
      <Text size="sm">{alternativa.condicaoPagamento ?? <NaoInformado />}</Text>
      {alternativa.prazoPagamentoBase && (
        <Text size="xs" c="dimmed">
          {`Prazo base do fornecedor: ${alternativa.prazoPagamentoBase}`}
        </Text>
      )}
    </>
  );
}

/** Crédito de cada tributo registrado no cálculo; crédito sem valor mostra a situação. */
function Creditos({ alternativa }: { alternativa: Alternativa }) {
  const creditos = alternativa.calculo?.creditos ?? [];
  if (creditos.length === 0) {
    return <NaoInformado />;
  }
  return (
    <Stack gap={0}>
      {creditos.map((credito) => (
        <Text key={credito.tributo} size="xs" style={{ whiteSpace: 'nowrap' }}>
          {`${rotuloTributo(credito.tributo)}: ${
            credito.valor === null
              ? apresentacaoSituacaoCredito(credito.situacao).rotulo
              : formatarMoeda(credito.valor)
          }`}
        </Text>
      ))}
    </Stack>
  );
}

function moedaOuAusente(valor: number | null, ausente = <NaoInformado />) {
  return valor === null ? ausente : formatarMoeda(valor);
}

const COLUNAS_CLASSIFICADAS: ColunaTabela<Alternativa>[] = [
  {
    titulo: 'Posição',
    semQuebra: true,
    conteudo: (alternativa) => (
      <Group gap={4} wrap="nowrap">
        <Badge color="teal" variant={alternativa.posicao === 1 ? 'filled' : 'light'} size="lg">
          {alternativa.posicao === null ? '—' : formatarPosicao(alternativa.posicao)}
        </Badge>
        {alternativa.empate && (
          <Badge color="yellow" variant="light">
            Empate
          </Badge>
        )}
      </Group>
    ),
  },
  { titulo: 'Fornecedor', conteudo: (alternativa) => <Fornecedor alternativa={alternativa} /> },
  {
    titulo: 'Condição de pagamento',
    conteudo: (alternativa) => <CondicaoPagamento alternativa={alternativa} />,
  },
  {
    titulo: 'Valor da operação',
    alinhamento: 'right',
    semQuebra: true,
    conteudo: (alternativa) => moedaOuAusente(alternativa.valorOperacao),
  },
  { titulo: 'Créditos', conteudo: (alternativa) => <Creditos alternativa={alternativa} /> },
  {
    titulo: 'Total de créditos',
    alinhamento: 'right',
    semQuebra: true,
    conteudo: (alternativa) => moedaOuAusente(alternativa.totalCreditos),
  },
  {
    titulo: 'Custo efetivo',
    alinhamento: 'right',
    semQuebra: true,
    conteudo: (alternativa) => (
      <Text fw={700} size="md">
        {moedaOuAusente(alternativa.custoEfetivo)}
      </Text>
    ),
  },
];

const COLUNAS_NAO_CLASSIFICADAS: ColunaTabela<Alternativa>[] = [
  { titulo: 'Fornecedor', conteudo: (alternativa) => <Fornecedor alternativa={alternativa} /> },
  {
    titulo: 'Situação',
    conteudo: (alternativa) => {
      const { explicacao } = apresentacaoSituacao(alternativa.situacao);
      return (
        <Stack gap={2}>
          <div>
            <SituacaoBadge alternativa={alternativa} />
          </div>
          {explicacao && (
            <Text size="xs" c="dimmed">
              {explicacao}
            </Text>
          )}
        </Stack>
      );
    },
  },
  {
    titulo: 'Motivo',
    conteudo: (alternativa) => <Text size="sm">{alternativa.motivo ?? <NaoInformado />}</Text>,
  },
  {
    titulo: 'Status do cálculo',
    semQuebra: true,
    conteudo: (alternativa) =>
      rotuloStatusCalculo(alternativa.statusCalculo) ?? (
        <Text span c="dimmed" size="sm">
          Não calculada
        </Text>
      ),
  },
  {
    titulo: 'Condição de pagamento',
    conteudo: (alternativa) => <CondicaoPagamento alternativa={alternativa} />,
  },
  {
    titulo: 'Valor da operação',
    alinhamento: 'right',
    semQuebra: true,
    conteudo: (alternativa) => moedaOuAusente(alternativa.valorOperacao),
  },
  {
    titulo: 'Custo calculado (fora do ranking)',
    alinhamento: 'right',
    semQuebra: true,
    conteudo: (alternativa) =>
      moedaOuAusente(
        alternativa.custoEfetivo,
        <Text span c="dimmed" size="sm">
          Não obtido
        </Text>,
      ),
  },
];

/** Configuração gravada na comparação: parâmetros (inclusive não definidos) e regras ativas. */
function ConfiguracaoComparacao({ comparacao }: { comparacao: Comparacao }) {
  const { parametros, regrasAtivas } = comparacao.configuracao;
  return (
    <section aria-labelledby="comparacao-configuracao">
      <Title id="comparacao-configuracao" order={2} size="h4" mb={4}>
        Configuração utilizada
      </Title>
      <Text size="sm" c="dimmed" mb="sm">
        Cópia dos parâmetros e das regras ativas no momento da comparação. Alterações posteriores
        não mudam este resultado.
      </Text>
      <Stack gap="md">
        <Paper withBorder p="lg">
          <Title order={3} size="h5" mb="sm">
            Parâmetros de cálculo
          </Title>
          {parametros.length === 0 ? (
            <Text size="sm" c="dimmed">
              Nenhum parâmetro registrado.
            </Text>
          ) : (
            <ListaDados
              dados={parametros.map((parametro) => ({
                rotulo: `${tituloDoParametro(parametro.chave)} (${parametro.chave})`,
                valor:
                  parametro.valor === null ? (
                    <Text span c="orange" size="sm" fw={500}>
                      Não definido
                    </Text>
                  ) : (
                    descreverValor(parametro.valor)
                  ),
              }))}
            />
          )}
        </Paper>
        {regrasAtivas.length === 0 ? (
          <Alert color="yellow" variant="light">
            Nenhuma regra tributária ativa estava configurada no momento da comparação.
          </Alert>
        ) : (
          <TabelaDados
            rotulo="Regras tributárias ativas na comparação"
            itens={regrasAtivas}
            obterChave={(regra) => regra.id}
            larguraMinima={560}
            colunas={[
              { titulo: 'Regra', conteudo: (regra) => regra.nome },
              {
                titulo: 'Tributo',
                semQuebra: true,
                conteudo: (regra) => rotuloTributo(regra.tributo),
              },
              {
                titulo: 'Versão',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (regra) => `v${String(regra.versao)}`,
              },
              {
                titulo: 'Prioridade',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (regra) => String(regra.prioridade),
              },
              {
                titulo: 'Id',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (regra) => `Nº ${String(regra.id)}`,
              },
            ]}
          />
        )}
      </Stack>
    </section>
  );
}
