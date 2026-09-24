import { Fieldset, Select, SimpleGrid, Stack, Text, TextInput } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { CHAVE_FORNECEDORES, fornecedoresApi } from '../../api/fornecedores';
import { chaveNfe, CHAVE_NFES, nfeApi } from '../../api/nfe';
import { useAuth } from '../../auth/useAuth';
import type { ErrosFormulario } from '../../hooks/useFormulario';
import { formatarCnpj, formatarDataHora } from '../../utils/formatacao';
import { OPCOES_ORIGEM_MERCADORIA } from '../regras-tributarias/opcoesCadastros';
import {
  CAMPOS_ALIQUOTAS,
  CAMPOS_VALORES,
  ROTULO_CAMPO_OPCAO,
  type ValoresOpcao,
} from './validacaoCotacao';

interface CamposOpcaoProps {
  valores: ValoresOpcao;
  erros: ErrosFormulario<ValoresOpcao>;
  alterar: (campo: keyof ValoresOpcao, valor: string) => void;
  /** Produto da cotação, para identificar os itens de NF-e vinculados a ele. */
  produtoId: number | null;
}

/**
 * Campos de uma opção de fornecedor (OpcaoCotacaoRequest). Valores e dados fiscais são
 * opcionais e seguem o formato do cálculo: quais deles são usados é decidido pelo backend,
 * conforme os parâmetros FONTE_VALORES_OPERACAO e FONTE_DADOS_FISCAIS.
 */
export function CamposOpcao({ valores, erros, alterar, produtoId }: CamposOpcaoProps) {
  const { isAdmin } = useAuth();
  const fornecedores = useQuery({
    queryKey: CHAVE_FORNECEDORES,
    queryFn: ({ signal }) => fornecedoresApi.listar(signal),
  });

  const opcoesFornecedor = (fornecedores.data ?? []).map((fornecedor) => ({
    value: String(fornecedor.id),
    label: `${fornecedor.razaoSocial} (${formatarCnpj(fornecedor.cnpj)})`,
  }));

  return (
    <Stack gap="md">
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
        <Select
          label="Fornecedor"
          description="Somente fornecedores ativos."
          placeholder={fornecedores.isPending ? 'Carregando…' : 'Selecione'}
          searchable
          clearable
          nothingFoundMessage="Nenhum fornecedor encontrado"
          data={opcoesFornecedor}
          value={valores.fornecedorId || null}
          error={
            erros.fornecedorId ??
            (fornecedores.isError ? 'Não foi possível carregar os fornecedores.' : undefined)
          }
          onChange={(valor) => {
            alterar('fornecedorId', valor ?? '');
          }}
        />
        <TextInput
          label="Condição de pagamento"
          description="Opcional. Ex.: 28/56/84 dias."
          value={valores.condicaoPagamento}
          error={erros.condicaoPagamento}
          onChange={(evento) => {
            alterar('condicaoPagamento', evento.currentTarget.value);
          }}
        />
      </SimpleGrid>

      {/* A consulta de NF-e é exclusiva do ADMIN no backend (GET /nfe). */}
      {isAdmin && (
        <SeletorItemNfe
          nfeItemId={valores.nfeItemId}
          erro={erros.nfeItemId}
          produtoId={produtoId}
          aoAlterar={(valor) => {
            alterar('nfeItemId', valor);
          }}
        />
      )}

      <TextInput
        label="Observação"
        description="Opcional."
        value={valores.observacao}
        error={erros.observacao}
        onChange={(evento) => {
          alterar('observacao', evento.currentTarget.value);
        }}
      />

      <Fieldset legend="Valores da operação (opcional)" variant="filled">
        <Text size="xs" c="dimmed" mb="sm">
          Valores da operação inteira, para a quantidade da cotação. Campo em branco fica como não
          informado (não é zero): informe 0 quando o componente não existir.
        </Text>
        <SimpleGrid cols={{ base: 1, xs: 2, sm: 3 }} spacing="md">
          {CAMPOS_VALORES.map((campo) => (
            <TextInput
              key={campo}
              label={ROTULO_CAMPO_OPCAO[campo]}
              inputMode="decimal"
              leftSection={
                <Text size="xs" c="dimmed">
                  R$
                </Text>
              }
              value={valores[campo]}
              error={erros[campo]}
              onChange={(evento) => {
                alterar(campo, evento.currentTarget.value);
              }}
            />
          ))}
        </SimpleGrid>
      </Fieldset>

      <Fieldset legend="Dados fiscais (opcional)" variant="filled">
        <Text size="xs" c="dimmed" mb="sm">
          Alíquotas destacadas na operação, em %. Campo em branco fica como não informado.
        </Text>
        <SimpleGrid cols={{ base: 1, xs: 2, sm: 3 }} spacing="md">
          <Select
            label="Origem da mercadoria"
            placeholder="Não informada"
            clearable
            data={OPCOES_ORIGEM_MERCADORIA}
            value={valores['dadosFiscais.origemMercadoria'] || null}
            error={erros['dadosFiscais.origemMercadoria']}
            onChange={(valor) => {
              alterar('dadosFiscais.origemMercadoria', valor ?? '');
            }}
          />
          <TextInput
            label="CFOP"
            inputMode="numeric"
            maxLength={4}
            value={valores['dadosFiscais.cfop']}
            error={erros['dadosFiscais.cfop']}
            onChange={(evento) => {
              alterar('dadosFiscais.cfop', evento.currentTarget.value);
            }}
          />
          {CAMPOS_ALIQUOTAS.map((campo) => (
            <TextInput
              key={campo}
              label={ROTULO_CAMPO_OPCAO[campo]}
              inputMode="decimal"
              value={valores[campo]}
              error={erros[campo]}
              onChange={(evento) => {
                alterar(campo, evento.currentTarget.value);
              }}
            />
          ))}
        </SimpleGrid>
      </Fieldset>
    </Stack>
  );
}

interface SeletorItemNfeProps {
  nfeItemId: string;
  erro?: string;
  produtoId: number | null;
  aoAlterar: (nfeItemId: string) => void;
}

/**
 * Item de NF-e de referência (somente ADMIN): escolhe a NF-e importada e depois o item.
 * Com item, o fornecedor da opção passa a ser o emitente da nota; o backend confere o
 * vínculo com o produto da cotação e se o emitente está ativo.
 */
function SeletorItemNfe({ nfeItemId, erro, produtoId, aoAlterar }: SeletorItemNfeProps) {
  const [nfeId, setNfeId] = useState('');
  const nfes = useQuery({
    queryKey: CHAVE_NFES,
    queryFn: ({ signal }) => nfeApi.listar(signal),
  });
  const nfe = useQuery({
    queryKey: chaveNfe(Number(nfeId)),
    queryFn: ({ signal }) => nfeApi.buscar(Number(nfeId), signal),
    enabled: nfeId !== '',
  });

  const opcoesNfe = (nfes.data ?? []).map((resumo) => ({
    value: String(resumo.id),
    label: `Nº ${String(resumo.numero)}/${String(resumo.serie)} — ${resumo.fornecedorRazaoSocial} (${formatarDataHora(resumo.dataEmissao)})`,
  }));
  const opcoesItem = (nfe.data?.itens ?? []).map((item) => ({
    value: String(item.id),
    label: `${String(item.numeroItem)}. ${item.descricao ?? 'Sem descrição'} — ${
      item.produto
        ? item.produto.id === produtoId
          ? item.produto.nome
          : `vinculado a outro produto (${item.produto.nome})`
        : 'sem produto vinculado'
    }`,
  }));

  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
      <Select
        label="NF-e de referência"
        description="Opcional. Somente para opções baseadas em uma NF-e importada."
        placeholder={nfes.isPending ? 'Carregando…' : 'Nenhuma'}
        searchable
        clearable
        nothingFoundMessage="Nenhuma NF-e encontrada"
        data={opcoesNfe}
        value={nfeId || null}
        error={nfes.isError ? 'Não foi possível carregar as NF-e.' : undefined}
        onChange={(valor) => {
          setNfeId(valor ?? '');
          aoAlterar('');
        }}
      />
      <Select
        label="Item da NF-e"
        description="O fornecedor da opção passa a ser o emitente da nota."
        placeholder={nfeId ? (nfe.isPending ? 'Carregando…' : 'Selecione') : 'Escolha a NF-e'}
        disabled={!nfeId}
        clearable
        data={opcoesItem}
        value={nfeItemId || null}
        error={erro ?? (nfe.isError ? 'Não foi possível carregar os itens da NF-e.' : undefined)}
        onChange={(valor) => {
          aoAlterar(valor ?? '');
        }}
      />
    </SimpleGrid>
  );
}
