import { useQuery } from '@tanstack/react-query';
import { CHAVE_FORNECEDORES, fornecedoresApi } from '../../api/fornecedores';
import { CHAVE_PRODUTOS, produtosApi } from '../../api/produtos';
import { ROTULO_TIPO_FORNECEDOR, TIPOS_FORNECEDOR, UFS } from '../../types/fornecedor';
import { ORIGENS_MERCADORIA, DESCRICAO_ORIGEM_MERCADORIA } from '../../types/nfe';
import { formatarCnpj } from '../../utils/formatacao';

export const OPCOES_UF = [...UFS].sort();

export const OPCOES_TIPO_FORNECEDOR = TIPOS_FORNECEDOR.map((tipo) => ({
  value: tipo,
  label: ROTULO_TIPO_FORNECEDOR[tipo],
}));

export const OPCOES_ORIGEM_MERCADORIA = ORIGENS_MERCADORIA.map((origem) => ({
  value: origem,
  label: `${origem} — ${DESCRICAO_ORIGEM_MERCADORIA[origem] ?? ''}`,
}));

interface Opcao {
  value: string;
  label: string;
}

/**
 * Fornecedores e produtos ativos para as condições das regras (mesmo cache das telas de
 * cadastro). Um id que não está entre os ativos continua selecionado e identificado como
 * inativo, para não sumir da tela; o backend decide se ele ainda pode ser usado.
 */
export function useCadastrosReferenciados() {
  const fornecedores = useQuery({
    queryKey: CHAVE_FORNECEDORES,
    queryFn: ({ signal }) => fornecedoresApi.listar(signal),
  });
  const produtos = useQuery({
    queryKey: CHAVE_PRODUTOS,
    queryFn: ({ signal }) => produtosApi.listar(signal),
  });

  const nomeFornecedor = (id: number) =>
    fornecedores.data?.find((fornecedor) => fornecedor.id === id)?.razaoSocial;
  const nomeProduto = (id: number) => produtos.data?.find((produto) => produto.id === id)?.nome;

  const opcoesFornecedor = (selecionado: string): Opcao[] =>
    comSelecionado(
      (fornecedores.data ?? []).map((fornecedor) => ({
        value: String(fornecedor.id),
        label: `${fornecedor.razaoSocial} (${formatarCnpj(fornecedor.cnpj)})`,
      })),
      selecionado,
      'Fornecedor',
    );
  const opcoesProduto = (selecionado: string): Opcao[] =>
    comSelecionado(
      (produtos.data ?? []).map((produto) => ({ value: String(produto.id), label: produto.nome })),
      selecionado,
      'Produto',
    );

  return { fornecedores, produtos, nomeFornecedor, nomeProduto, opcoesFornecedor, opcoesProduto };
}

function comSelecionado(opcoes: Opcao[], selecionado: string, tipo: string): Opcao[] {
  if (!selecionado || opcoes.some((opcao) => opcao.value === selecionado)) {
    return opcoes;
  }
  return [...opcoes, { value: selecionado, label: `${tipo} #${selecionado} (inativo)` }];
}
