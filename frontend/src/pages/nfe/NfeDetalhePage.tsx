import { Accordion, Alert, Badge, Button, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router';
import { chaveNfe, nfeApi } from '../../api/nfe';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { CarregandoPagina } from '../../components/CarregandoPagina';
import { ListaDados } from '../../components/ListaDados';
import { MensagemErro } from '../../components/MensagemErro';
import { NaoInformado, TabelaDados } from '../../components/TabelaDados';
import { ROTAS } from '../../routes/rotas';
import { DESCRICAO_ORIGEM_MERCADORIA, type Nfe, type NfeItem } from '../../types/nfe';
import {
  formatarChaveAcesso,
  formatarCnpj,
  formatarDataHora,
  formatarMoeda,
  formatarNumero,
  formatarPercentual,
} from '../../utils/formatacao';

/** Valores ausentes na nota ficam como "Não informado"; nada é presumido nem recalculado. */
function moeda(valor: number | null) {
  return valor === null ? null : formatarMoeda(valor);
}

function opcional(valor: number | null, formatar: (v: number) => string) {
  return valor === null ? <NaoInformado /> : formatar(valor);
}

function VoltarParaListagem() {
  return (
    <Button component={Link} to={ROTAS.nfe} variant="default" size="xs">
      Voltar para NF-e
    </Button>
  );
}

/** Detalhes de uma NF-e importada (RF06, RF07), exatamente como devolvidos pelo backend. */
export function NfeDetalhePage() {
  const { id } = useParams();
  const idNumerico = Number(id);
  const idValido = Number.isInteger(idNumerico) && idNumerico > 0;

  const consulta = useQuery({
    queryKey: chaveNfe(idNumerico),
    queryFn: ({ signal }) => nfeApi.buscar(idNumerico, signal),
    enabled: idValido,
  });

  if (!idValido) {
    return (
      <>
        <CabecalhoPagina titulo="NF-e não encontrada" acoes={<VoltarParaListagem />} />
        <Text>O endereço acessado não corresponde a uma NF-e.</Text>
      </>
    );
  }

  if (consulta.isPending) {
    return (
      <>
        <CabecalhoPagina titulo="Detalhes da NF-e" acoes={<VoltarParaListagem />} />
        <CarregandoPagina mensagem="Carregando NF-e…" />
      </>
    );
  }

  if (consulta.isError) {
    return (
      <>
        <CabecalhoPagina titulo="Detalhes da NF-e" acoes={<VoltarParaListagem />} />
        <MensagemErro
          erro={consulta.error}
          titulo="Não foi possível carregar a NF-e"
          aoTentarNovamente={() => {
            void consulta.refetch();
          }}
        />
      </>
    );
  }

  return <DetalhesNfe nfe={consulta.data} />;
}

function DetalhesNfe({ nfe }: { nfe: Nfe }) {
  const { totais } = nfe;
  return (
    <>
      <CabecalhoPagina
        titulo={`NF-e nº ${String(nfe.numero)} (série ${String(nfe.serie)})`}
        descricao="Dados extraídos do XML. Os valores de impostos são os destacados na nota, e não créditos calculados."
        acoes={<VoltarParaListagem />}
      />

      <Stack gap="lg">
        <Paper withBorder p="lg" component="section" aria-labelledby="nfe-identificacao">
          <Title id="nfe-identificacao" order={2} size="h4" mb="md">
            Identificação
          </Title>
          <ListaDados
            dados={[
              { rotulo: 'Chave de acesso', valor: formatarChaveAcesso(nfe.chaveAcesso) },
              { rotulo: 'Data de emissão', valor: formatarDataHora(nfe.dataEmissao) },
              { rotulo: 'Natureza da operação', valor: nfe.naturezaOperacao },
              { rotulo: 'Fornecedor', valor: nfe.fornecedor.razaoSocial },
              { rotulo: 'CNPJ do emitente', valor: formatarCnpj(nfe.emitenteCnpj) },
              { rotulo: 'UF do emitente', valor: nfe.emitenteUf },
              {
                rotulo: 'CNPJ do destinatário',
                valor: nfe.destinatarioCnpj && formatarCnpj(nfe.destinatarioCnpj),
              },
              { rotulo: 'UF do destinatário', valor: nfe.destinatarioUf },
              { rotulo: 'Importada em', valor: formatarDataHora(nfe.importadoEm) },
            ]}
          />
        </Paper>

        <Paper withBorder p="lg" component="section" aria-labelledby="nfe-totais">
          <Title id="nfe-totais" order={2} size="h4" mb="md">
            Totais da nota
          </Title>
          <ListaDados
            dados={[
              { rotulo: 'Valor dos produtos', valor: moeda(totais.valorProdutos) },
              { rotulo: 'Frete', valor: moeda(totais.valorFrete) },
              { rotulo: 'Seguro', valor: moeda(totais.valorSeguro) },
              { rotulo: 'Desconto', valor: moeda(totais.valorDesconto) },
              { rotulo: 'Outras despesas', valor: moeda(totais.valorOutrasDespesas) },
              { rotulo: 'IPI', valor: moeda(totais.valorIpi) },
              { rotulo: 'Valor total da nota', valor: moeda(totais.valorTotal) },
            ]}
            colunas={4}
          />
        </Paper>

        <section aria-labelledby="nfe-itens">
          <Title id="nfe-itens" order={2} size="h4" mb="sm">
            {`Itens (${String(nfe.itens.length)})`}
          </Title>
          {nfe.itensSemProduto > 0 && (
            <Alert color="yellow" variant="light" mb="sm">
              {nfe.itensSemProduto === 1
                ? '1 item não está vinculado a um produto cadastrado.'
                : `${String(nfe.itensSemProduto)} itens não estão vinculados a um produto cadastrado.`}{' '}
              O vínculo é feito somente pelo GTIN/EAN idêntico ao de um produto ativo.
            </Alert>
          )}
          {nfe.itens.length === 0 ? (
            <Text c="dimmed">A NF-e não possui itens.</Text>
          ) : (
            <Accordion multiple variant="separated" chevronPosition="left">
              {nfe.itens.map((item) => (
                <ItemNfe key={item.id} item={item} />
              ))}
            </Accordion>
          )}
        </section>
      </Stack>
    </>
  );
}

interface LinhaTributo {
  tributo: string;
  origem: string | null;
  cst: string | null;
  baseCalculo: number | null;
  aliquota: number | null;
  valor: number | null;
}

function linhasTributos(item: NfeItem): LinhaTributo[] {
  const { icms } = item;
  return [
    {
      tributo: 'ICMS',
      origem: icms.origem,
      // Emitente do Simples Nacional informa CSOSN em vez de CST.
      cst: icms.cst ?? (icms.csosn ? `CSOSN ${icms.csosn}` : null),
      baseCalculo: icms.baseCalculo,
      aliquota: icms.aliquota,
      valor: icms.valor,
    },
    { tributo: 'IPI', origem: null, ...item.ipi },
    { tributo: 'PIS', origem: null, ...item.pis },
    { tributo: 'COFINS', origem: null, ...item.cofins },
  ];
}

function ItemNfe({ item }: { item: NfeItem }) {
  const descricao = item.descricao ?? 'Sem descrição';
  return (
    <Accordion.Item value={String(item.id)}>
      <Accordion.Control aria-label={`Item ${String(item.numeroItem)}: ${descricao}`}>
        <Group justify="space-between" wrap="wrap" gap="xs" pr="sm">
          <Text size="sm" fw={500}>
            {`${String(item.numeroItem)}. ${descricao}`}
          </Text>
          <Group gap="xs" wrap="nowrap">
            {item.produto ? (
              <Badge variant="light" color="teal">
                {item.produto.nome}
              </Badge>
            ) : (
              <Badge variant="light" color="yellow">
                Sem produto vinculado
              </Badge>
            )}
            {item.valorProduto !== null && (
              <Text size="sm" style={{ whiteSpace: 'nowrap' }}>
                {formatarMoeda(item.valorProduto)}
              </Text>
            )}
          </Group>
        </Group>
      </Accordion.Control>
      <Accordion.Panel>
        <Stack gap="md">
          <ListaDados
            colunas={4}
            dados={[
              { rotulo: 'Produto vinculado', valor: item.produto?.nome },
              { rotulo: 'Código no fornecedor', valor: item.codigoProdutoFornecedor },
              { rotulo: 'GTIN/EAN', valor: item.gtin },
              { rotulo: 'NCM', valor: item.ncm },
              { rotulo: 'CFOP', valor: item.cfop },
              { rotulo: 'Unidade', valor: item.unidade },
              {
                rotulo: 'Quantidade',
                valor: item.quantidade === null ? null : formatarNumero(item.quantidade),
              },
              { rotulo: 'Valor unitário', valor: moeda(item.valorUnitario) },
              { rotulo: 'Valor dos produtos', valor: moeda(item.valorProduto) },
              { rotulo: 'Frete', valor: moeda(item.valorFrete) },
              { rotulo: 'Seguro', valor: moeda(item.valorSeguro) },
              { rotulo: 'Desconto', valor: moeda(item.valorDesconto) },
              { rotulo: 'Outras despesas', valor: moeda(item.valorOutrasDespesas) },
            ]}
          />
          <TabelaDados
            rotulo={`Tributos destacados no item ${String(item.numeroItem)}`}
            itens={linhasTributos(item)}
            obterChave={(linha) => linha.tributo}
            larguraMinima={560}
            colunas={[
              { titulo: 'Tributo', conteudo: (linha) => <Text fw={500}>{linha.tributo}</Text> },
              {
                titulo: 'Origem',
                conteudo: (linha) =>
                  linha.tributo !== 'ICMS' ? (
                    '—'
                  ) : linha.origem === null ? (
                    <NaoInformado />
                  ) : (
                    <span title={DESCRICAO_ORIGEM_MERCADORIA[linha.origem]}>{linha.origem}</span>
                  ),
              },
              {
                titulo: 'CST',
                semQuebra: true,
                conteudo: (linha) => linha.cst ?? <NaoInformado />,
              },
              {
                titulo: 'Base de cálculo',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (linha) => opcional(linha.baseCalculo, formatarMoeda),
              },
              {
                titulo: 'Alíquota',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (linha) => opcional(linha.aliquota, formatarPercentual),
              },
              {
                titulo: 'Valor',
                alinhamento: 'right',
                semQuebra: true,
                conteudo: (linha) => opcional(linha.valor, formatarMoeda),
              },
            ]}
          />
          {item.icms.origem !== null && DESCRICAO_ORIGEM_MERCADORIA[item.icms.origem] && (
            <Text size="xs" c="dimmed">
              {`Origem ${item.icms.origem} (tag orig): ${DESCRICAO_ORIGEM_MERCADORIA[item.icms.origem] ?? ''}.`}
            </Text>
          )}
        </Stack>
      </Accordion.Panel>
    </Accordion.Item>
  );
}
