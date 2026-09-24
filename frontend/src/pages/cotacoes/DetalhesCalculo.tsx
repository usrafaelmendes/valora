import { Badge, Stack, Text, Title } from '@mantine/core';
import { ListaDados } from '../../components/ListaDados';
import { NaoInformado, TabelaDados } from '../../components/TabelaDados';
import { ROTULO_TIPO_FORNECEDOR } from '../../types/fornecedor';
import type { Calculo, CalculoParametros } from '../../types/cotacao';
import { DESCRICAO_ORIGEM_MERCADORIA } from '../../types/nfe';
import {
  formatarDataHora,
  formatarMoeda,
  formatarNumero,
  formatarPercentual,
} from '../../utils/formatacao';
import { descreverValor, tituloDoParametro } from '../parametros-calculo/apresentacaoParametro';
import {
  apresentacaoSituacaoCredito,
  rotuloFormaAliquota,
  rotuloPendencia,
  rotuloStatusCalculo,
  rotuloTributo,
} from './apresentacaoComparacao';

/** Valor ausente vira null (exibido como "Não informado"); nunca zero presumido. */
function moeda(valor: number | null) {
  return valor === null ? null : formatarMoeda(valor);
}

function percentual(valor: number | null) {
  return valor === null ? null : formatarPercentual(valor);
}

function naoDefinido() {
  return (
    <Text span c="dimmed" size="sm">
      Não definido
    </Text>
  );
}

/** Parâmetros gravados no cálculo, na mesma ordem e com as chaves de /parametros-calculo. */
const PARAMETROS_CALCULO: [keyof CalculoParametros, string][] = [
  ['fonteValoresOperacao', 'FONTE_VALORES_OPERACAO'],
  ['fonteDadosFiscais', 'FONTE_DADOS_FISCAIS'],
  ['fonteUfOrigem', 'FONTE_UF_ORIGEM'],
  ['composicaoValorOperacao', 'COMPOSICAO_VALOR_OPERACAO'],
  ['composicaoBaseCreditos', 'COMPOSICAO_BASE_CREDITOS'],
  ['arredondamentoCreditos', 'ARREDONDAMENTO_CREDITOS'],
  ['criterioArredondamento', 'CRITERIO_ARREDONDAMENTO'],
];

/**
 * Rastreabilidade completa de um cálculo gravado (REGRAS_TRIBUTARIAS §10): operação,
 * parâmetros vigentes, valores e alíquotas usados, cada crédito com a regra e a versão,
 * totais, arredondamento e pendências. Nada é recalculado: tudo vem do backend.
 */
export function DetalhesCalculo({ calculo }: { calculo: Calculo }) {
  const { operacao, componentes, aliquotasOperacao } = calculo;
  const origem = operacao.origemMercadoria;

  return (
    <Stack gap="lg">
      <section>
        <Title order={4} size="h6" mb="xs">
          Operação calculada
        </Title>
        <ListaDados
          colunas={4}
          dados={[
            { rotulo: 'Cálculo', valor: `Nº ${String(calculo.id)}` },
            {
              rotulo: 'Status do cálculo',
              valor: rotuloStatusCalculo(calculo.status),
            },
            { rotulo: 'Executado em', valor: formatarDataHora(calculo.executadoEm) },
            {
              rotulo: 'Tipo do fornecedor',
              valor: operacao.tipoFornecedor && ROTULO_TIPO_FORNECEDOR[operacao.tipoFornecedor],
            },
            { rotulo: 'UF de origem', valor: operacao.ufOrigem },
            { rotulo: 'UF de destino', valor: operacao.ufDestino },
            {
              rotulo: 'Origem da mercadoria',
              valor:
                origem &&
                `${origem}${DESCRICAO_ORIGEM_MERCADORIA[origem] ? ` — ${DESCRICAO_ORIGEM_MERCADORIA[origem] ?? ''}` : ''}`,
            },
            { rotulo: 'CFOP', valor: operacao.cfop },
            {
              rotulo: 'Quantidade',
              valor: operacao.quantidade === null ? null : formatarNumero(operacao.quantidade),
            },
            {
              rotulo: 'Item de NF-e da operação',
              valor: operacao.nfeItemId === null ? null : `Nº ${String(operacao.nfeItemId)}`,
            },
            {
              rotulo: 'Item de NF-e dos dados fiscais',
              valor:
                operacao.nfeItemDadosFiscaisId === null
                  ? null
                  : `Nº ${String(operacao.nfeItemDadosFiscaisId)}`,
            },
          ]}
        />
      </section>

      <section>
        <Title order={4} size="h6" mb="xs">
          Parâmetros usados no cálculo
        </Title>
        <ListaDados
          dados={PARAMETROS_CALCULO.map(([campo, chave]) => {
            const valor = calculo.parametros[campo];
            return {
              rotulo: tituloDoParametro(chave),
              valor: valor === null ? naoDefinido() : descreverValor(valor),
            };
          })}
        />
      </section>

      <section>
        <Title order={4} size="h6" mb="xs">
          Valores e alíquotas da operação
        </Title>
        <ListaDados
          colunas={4}
          dados={[
            { rotulo: 'Valor dos produtos', valor: moeda(componentes.valorProduto) },
            { rotulo: 'IPI', valor: moeda(componentes.valorIpi) },
            { rotulo: 'Frete', valor: moeda(componentes.valorFrete) },
            { rotulo: 'Seguro', valor: moeda(componentes.valorSeguro) },
            { rotulo: 'Outras despesas', valor: moeda(componentes.valorOutrasDespesas) },
            { rotulo: 'Desconto', valor: moeda(componentes.valorDesconto) },
            { rotulo: 'Alíquota de ICMS', valor: percentual(aliquotasOperacao.icms) },
            { rotulo: 'Alíquota de IPI', valor: percentual(aliquotasOperacao.ipi) },
            { rotulo: 'Alíquota de PIS', valor: percentual(aliquotasOperacao.pis) },
            { rotulo: 'Alíquota de COFINS', valor: percentual(aliquotasOperacao.cofins) },
            { rotulo: 'Valor da operação', valor: moeda(calculo.valorOperacao) },
            { rotulo: 'Base dos créditos', valor: moeda(calculo.baseCreditos) },
          ]}
        />
      </section>

      <section>
        <Title order={4} size="h6" mb="xs">
          Créditos por tributo
        </Title>
        {calculo.creditos.length === 0 ? (
          <Text size="sm" c="dimmed">
            Nenhum crédito registrado no cálculo.
          </Text>
        ) : (
          <TabelaDados
            rotulo={`Créditos do cálculo ${String(calculo.id)}`}
            itens={calculo.creditos}
            obterChave={(credito) => credito.tributo}
            larguraMinima={1100}
            colunas={[
              {
                titulo: 'Tributo',
                semQuebra: true,
                conteudo: (credito) => <Text fw={500}>{rotuloTributo(credito.tributo)}</Text>,
              },
              {
                titulo: 'Situação',
                semQuebra: true,
                conteudo: (credito) => {
                  const { rotulo, cor } = apresentacaoSituacaoCredito(credito.situacao);
                  return (
                    <Badge variant="light" color={cor}>
                      {rotulo}
                    </Badge>
                  );
                },
              },
              {
                titulo: 'Regra (versão)',
                conteudo: (credito) =>
                  credito.regra ? (
                    <>
                      <Text size="sm">
                        {`${credito.regra.nome ?? `Regra nº ${String(credito.regra.id)}`} (v${String(credito.regra.versao ?? '?')})`}
                      </Text>
                      <Text size="xs" c="dimmed">
                        {`Regra nº ${String(credito.regra.id)}${credito.regra.formaAliquota ? ` · ${rotuloFormaAliquota(credito.regra.formaAliquota) ?? ''}` : ''}`}
                      </Text>
                    </>
                  ) : (
                    <NaoInformado />
                  ),
              },
              {
                titulo: 'Alíquota obtida',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (credito) => percentual(credito.aliquotaObtida) ?? <NaoInformado />,
              },
              {
                titulo: 'Fator',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (credito) =>
                  credito.fator === null ? <NaoInformado /> : formatarNumero(credito.fator),
              },
              {
                titulo: 'Alíquota aplicada',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (credito) => percentual(credito.aliquotaAplicada) ?? <NaoInformado />,
              },
              {
                titulo: 'Base',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (credito) => moeda(credito.baseCalculo) ?? <NaoInformado />,
              },
              {
                titulo: 'Sem arredondamento',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (credito) => moeda(credito.valorSemArredondamento) ?? <NaoInformado />,
              },
              {
                titulo: 'Crédito',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (credito) =>
                  credito.valor === null ? (
                    <NaoInformado />
                  ) : (
                    <Text fw={600}>{formatarMoeda(credito.valor)}</Text>
                  ),
              },
              {
                titulo: 'Observação',
                conteudo: (credito) => {
                  const textos = [
                    credito.mensagem,
                    credito.regrasEmConflito && `Regras em conflito: ${credito.regrasEmConflito}`,
                  ].filter(Boolean);
                  return textos.length === 0 ? '—' : <Text size="xs">{textos.join(' ')}</Text>;
                },
              },
            ]}
          />
        )}
      </section>

      <section>
        <Title order={4} size="h6" mb="xs">
          Totais
        </Title>
        <ListaDados
          colunas={4}
          dados={[
            {
              rotulo: 'Total de créditos sem arredondamento',
              valor: moeda(calculo.totalCreditosSemArredondamento),
            },
            { rotulo: 'Total de créditos', valor: moeda(calculo.totalCreditos) },
            {
              rotulo: 'Diferença de arredondamento',
              valor: moeda(calculo.diferencaArredondamento),
            },
            {
              rotulo: 'Custo efetivo',
              valor:
                calculo.custoEfetivo === null ? (
                  <Text span c="orange" size="sm" fw={500}>
                    Não obtido
                  </Text>
                ) : (
                  <Text span fw={700}>
                    {formatarMoeda(calculo.custoEfetivo)}
                  </Text>
                ),
            },
          ]}
        />
      </section>

      <section>
        <Title order={4} size="h6" mb="xs">
          Pendências e avisos
        </Title>
        {calculo.pendencias.length === 0 ? (
          <Text size="sm" c="dimmed">
            Nenhuma pendência ou aviso registrado.
          </Text>
        ) : (
          <TabelaDados
            rotulo={`Pendências do cálculo ${String(calculo.id)}`}
            itens={calculo.pendencias.map((pendencia, indice) => ({ ...pendencia, indice }))}
            obterChave={(pendencia) => pendencia.indice}
            larguraMinima={600}
            colunas={[
              {
                titulo: 'Tipo',
                semQuebra: true,
                conteudo: (pendencia) => rotuloPendencia(pendencia.tipo),
              },
              {
                titulo: 'Efeito',
                semQuebra: true,
                conteudo: (pendencia) =>
                  pendencia.bloqueante ? (
                    <Badge color="red" variant="light">
                      Bloqueante
                    </Badge>
                  ) : (
                    <Badge color="gray" variant="light">
                      Aviso
                    </Badge>
                  ),
              },
              { titulo: 'Mensagem', conteudo: (pendencia) => pendencia.mensagem },
            ]}
          />
        )}
      </section>
    </Stack>
  );
}
