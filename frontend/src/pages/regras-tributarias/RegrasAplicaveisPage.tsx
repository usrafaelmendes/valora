import {
  Alert,
  Badge,
  Button,
  Group,
  Paper,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useMutation } from '@tanstack/react-query';
import type { SubmitEvent } from 'react';
import { Link } from 'react-router';
import { regrasTributariasApi } from '../../api/regrasTributarias';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { ListaDados } from '../../components/ListaDados';
import { MensagemErro } from '../../components/MensagemErro';
import { TabelaDados } from '../../components/TabelaDados';
import { useFormulario, type ErrosFormulario } from '../../hooks/useFormulario';
import { ROTAS } from '../../routes/rotas';
import { ROTULO_TIPO_FORNECEDOR, type TipoFornecedor, type Uf } from '../../types/fornecedor';
import {
  ROTULO_TRIBUTO,
  type RegrasAplicaveisRequest,
  type RegrasAplicaveisResponse,
  type SelecaoTributo,
  type SituacaoSelecao,
} from '../../types/regraTributaria';
import { ehTipoFornecedor, ehUf } from '../fornecedores/validacaoFornecedor';
import { descreverFator, descreverTaxa } from './apresentacaoRegra';
import {
  OPCOES_ORIGEM_MERCADORIA,
  OPCOES_TIPO_FORNECEDOR,
  OPCOES_UF,
  useCadastrosReferenciados,
} from './opcoesCadastros';

interface ValoresOperacao extends Record<string, string> {
  fornecedorId: string;
  tipoFornecedor: TipoFornecedor | '';
  produtoId: string;
  ufOrigem: Uf | '';
  ufDestino: Uf | '';
  origemMercadoria: string;
  cfop: string;
}

const VALORES_INICIAIS: ValoresOperacao = {
  fornecedorId: '',
  tipoFornecedor: '',
  produtoId: '',
  ufOrigem: '',
  ufDestino: '',
  origemMercadoria: '',
  cfop: '',
};

const CAMPOS_OPERACAO = Object.keys(VALORES_INICIAIS);

function validarOperacao(valores: ValoresOperacao): ErrosFormulario<ValoresOperacao> {
  const cfop = valores.cfop.trim();
  return { cfop: cfop && !/^\d{4}$/.test(cfop) ? 'O CFOP deve ter 4 dígitos.' : undefined };
}

function paraRequest(valores: ValoresOperacao): RegrasAplicaveisRequest {
  return {
    fornecedorId: valores.fornecedorId ? Number(valores.fornecedorId) : null,
    tipoFornecedor: valores.tipoFornecedor || null,
    produtoId: valores.produtoId ? Number(valores.produtoId) : null,
    ufOrigem: valores.ufOrigem || null,
    ufDestino: valores.ufDestino || null,
    origemMercadoria: valores.origemMercadoria || null,
    cfop: valores.cfop.trim() || null,
  };
}

const APRESENTACAO_SITUACAO: Record<SituacaoSelecao, { rotulo: string; cor: string }> = {
  APLICAVEL: { rotulo: 'Regra aplicável', cor: 'teal' },
  SEM_REGRA: { rotulo: 'Sem regra', cor: 'yellow' },
  CONFLITO: { rotulo: 'Conflito', cor: 'red' },
};

const DESCONHECIDO = 'Não informado';

/**
 * Conferência da configuração (POST /regras-tributarias/aplicaveis): mostra, para uma operação
 * de exemplo, a regra que o backend selecionaria em cada tributo. Não calcula créditos, e o
 * frontend não decide nada: apenas exibe a seleção devolvida, inclusive conflitos.
 */
export function RegrasAplicaveisPage() {
  const cadastros = useCadastrosReferenciados();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    VALORES_INICIAIS,
    validarOperacao,
  );

  const conferir = useMutation({
    mutationFn: (dados: RegrasAplicaveisRequest) => regrasTributariasApi.aplicaveis(dados),
    onError: aplicarErrosApi,
  });

  const enviar = (evento: SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (validarTudo()) {
      conferir.mutate(paraRequest(valores));
    }
  };

  return (
    <>
      <CabecalhoPagina
        titulo="Conferir regras aplicáveis"
        descricao="Informe as características de uma operação de exemplo para ver qual regra ativa seria usada em cada tributo. Campos vazios são tratados como desconhecidos: somente regras sem condição sobre eles podem se aplicar."
        acoes={
          <Button component={Link} to={ROTAS.regrasTributarias} variant="default" size="xs">
            Voltar para regras
          </Button>
        }
      />

      <Stack gap="lg">
        <Paper withBorder p="lg" component="form" onSubmit={enviar} noValidate>
          <Stack gap="md">
            <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="md">
              <Select
                label="Fornecedor"
                placeholder={cadastros.fornecedores.isPending ? 'Carregando…' : DESCONHECIDO}
                clearable
                searchable
                nothingFoundMessage="Nenhum fornecedor encontrado"
                data={cadastros.opcoesFornecedor(valores.fornecedorId)}
                value={valores.fornecedorId || null}
                error={erros.fornecedorId}
                onChange={(valor) => {
                  alterar('fornecedorId', valor ?? '');
                }}
              />
              <Select
                label="Tipo de fornecedor"
                placeholder={DESCONHECIDO}
                clearable
                data={OPCOES_TIPO_FORNECEDOR}
                value={valores.tipoFornecedor || null}
                error={erros.tipoFornecedor}
                onChange={(valor) => {
                  alterar('tipoFornecedor', ehTipoFornecedor(valor) ? valor : '');
                }}
              />
              <Select
                label="Produto"
                placeholder={cadastros.produtos.isPending ? 'Carregando…' : DESCONHECIDO}
                clearable
                searchable
                nothingFoundMessage="Nenhum produto encontrado"
                data={cadastros.opcoesProduto(valores.produtoId)}
                value={valores.produtoId || null}
                error={erros.produtoId}
                onChange={(valor) => {
                  alterar('produtoId', valor ?? '');
                }}
              />
              <Select
                label="UF de origem"
                placeholder={DESCONHECIDO}
                clearable
                searchable
                data={OPCOES_UF}
                value={valores.ufOrigem || null}
                error={erros.ufOrigem}
                onChange={(valor) => {
                  alterar('ufOrigem', ehUf(valor) ? valor : '');
                }}
              />
              <Select
                label="UF de destino"
                description="Vazio: usa o parâmetro UF_DESTINO."
                placeholder="Parâmetro UF_DESTINO"
                clearable
                searchable
                data={OPCOES_UF}
                value={valores.ufDestino || null}
                error={erros.ufDestino}
                onChange={(valor) => {
                  alterar('ufDestino', ehUf(valor) ? valor : '');
                }}
              />
              <TextInput
                label="CFOP"
                placeholder={DESCONHECIDO}
                inputMode="numeric"
                maxLength={4}
                value={valores.cfop}
                error={erros.cfop}
                onChange={(evento) => {
                  alterar('cfop', evento.currentTarget.value);
                }}
              />
            </SimpleGrid>
            <Select
              label="Origem da mercadoria (tag orig)"
              placeholder={DESCONHECIDO}
              clearable
              data={OPCOES_ORIGEM_MERCADORIA}
              value={valores.origemMercadoria || null}
              error={erros.origemMercadoria}
              onChange={(valor) => {
                alterar('origemMercadoria', valor ?? '');
              }}
            />
            {conferir.isError && (
              <MensagemErro
                erro={conferir.error}
                titulo="Não foi possível conferir as regras"
                camposOcultos={CAMPOS_OPERACAO}
              />
            )}
            <Group justify="flex-end">
              <Button type="submit" loading={conferir.isPending}>
                Conferir
              </Button>
            </Group>
          </Stack>
        </Paper>

        {conferir.data && (
          <ResultadoAplicabilidade
            resultado={conferir.data}
            nomeFornecedor={cadastros.nomeFornecedor}
            nomeProduto={cadastros.nomeProduto}
          />
        )}
      </Stack>
    </>
  );
}

interface ResultadoAplicabilidadeProps {
  resultado: RegrasAplicaveisResponse;
  nomeFornecedor: (id: number) => string | undefined;
  nomeProduto: (id: number) => string | undefined;
}

function ResultadoAplicabilidade({
  resultado,
  nomeFornecedor,
  nomeProduto,
}: ResultadoAplicabilidadeProps) {
  const { operacao } = resultado;
  return (
    <Stack gap="md" component="section" aria-labelledby="resultado-aplicaveis">
      <Title id="resultado-aplicaveis" order={2} size="h4">
        Resultado
      </Title>
      <Paper withBorder p="lg">
        <Text size="sm" fw={500} mb="sm">
          Operação considerada pelo backend
        </Text>
        <ListaDados
          colunas={4}
          dados={[
            {
              rotulo: 'Fornecedor',
              valor:
                operacao.fornecedorId === null
                  ? null
                  : (nomeFornecedor(operacao.fornecedorId) ?? `#${String(operacao.fornecedorId)}`),
            },
            {
              rotulo: 'Tipo de fornecedor',
              valor: operacao.tipoFornecedor && ROTULO_TIPO_FORNECEDOR[operacao.tipoFornecedor],
            },
            {
              rotulo: 'Produto',
              valor:
                operacao.produtoId === null
                  ? null
                  : (nomeProduto(operacao.produtoId) ?? `#${String(operacao.produtoId)}`),
            },
            { rotulo: 'UF de origem', valor: operacao.ufOrigem },
            { rotulo: 'UF de destino', valor: operacao.ufDestino },
            { rotulo: 'Origem da mercadoria', valor: operacao.origemMercadoria },
            { rotulo: 'CFOP', valor: operacao.cfop },
          ]}
        />
      </Paper>
      {resultado.tributos.map((selecao) => (
        <SelecaoDoTributo key={selecao.tributo} selecao={selecao} />
      ))}
    </Stack>
  );
}

function SelecaoDoTributo({ selecao }: { selecao: SelecaoTributo }) {
  const situacao = APRESENTACAO_SITUACAO[selecao.situacao];
  const titulo = ROTULO_TRIBUTO[selecao.tributo];
  return (
    <Paper
      withBorder
      p="lg"
      component="article"
      aria-label={`${titulo}: ${situacao.rotulo}`}
      style={
        selecao.situacao === 'APLICAVEL'
          ? undefined
          : { borderColor: `var(--mantine-color-${situacao.cor}-6)` }
      }
    >
      <Group justify="space-between" mb={selecao.regras.length > 0 || selecao.mensagem ? 'sm' : 0}>
        <Title order={3} size="h5">
          {titulo}
        </Title>
        <Badge color={situacao.cor} variant="light">
          {situacao.rotulo}
        </Badge>
      </Group>
      {selecao.mensagem && (
        <Alert color={situacao.cor} variant="light" mb={selecao.regras.length > 0 ? 'sm' : 0}>
          {selecao.mensagem}
        </Alert>
      )}
      {selecao.regras.length > 0 && (
        <TabelaDados
          rotulo={
            selecao.situacao === 'CONFLITO'
              ? `Regras em conflito para ${titulo}`
              : `Regra aplicável para ${titulo}`
          }
          itens={selecao.regras}
          obterChave={(regra) => regra.id}
          larguraMinima={560}
          colunas={[
            {
              titulo: 'Regra',
              conteudo: (regra) => <Text fw={500}>{regra.nome}</Text>,
            },
            { titulo: 'Tributo da regra', conteudo: (regra) => ROTULO_TRIBUTO[regra.tributo] },
            { titulo: 'Origem da taxa', conteudo: (regra) => descreverTaxa(regra) },
            {
              titulo: 'Fator',
              alinhamento: 'right',
              conteudo: (regra) => descreverFator(regra.fator),
            },
            { titulo: 'Prioridade', alinhamento: 'right', conteudo: (regra) => regra.prioridade },
            {
              titulo: 'Versão',
              alinhamento: 'right',
              conteudo: (regra) => `v${String(regra.versao)}`,
            },
          ]}
        />
      )}
    </Paper>
  );
}
