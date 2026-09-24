import { excedeLimite, textoOpcional, type ErrosFormulario } from '../../hooks/useFormulario';
import type { Produto, ProdutoRequest } from '../../types/produto';

/** Valores do formulário de produto, como digitados. */
export interface ValoresProduto extends Record<string, string> {
  nome: string;
  descricao: string;
  gtin: string;
}

export const CAMPOS_PRODUTO: string[] = ['nome', 'descricao', 'gtin'];

/** Mesmo formato aceito pelo backend; o dígito verificador é conferido pela API. */
const FORMATO_GTIN = /^(\d{8}|\d{12,14})$/;

export function valoresIniciaisProduto(produto: Produto | null): ValoresProduto {
  return {
    nome: produto?.nome ?? '',
    descricao: produto?.descricao ?? '',
    gtin: produto?.gtin ?? '',
  };
}

/** Espelha as restrições de ProdutoRequest, com as mesmas mensagens do backend. */
export function validarProduto(valores: ValoresProduto): ErrosFormulario<ValoresProduto> {
  const gtin = valores.gtin.trim();
  return {
    nome: !valores.nome.trim()
      ? 'O nome é obrigatório.'
      : excedeLimite(valores.nome, 150, 'O nome deve ter no máximo 150 caracteres.'),
    descricao: excedeLimite(
      valores.descricao,
      500,
      'A descrição deve ter no máximo 500 caracteres.',
    ),
    gtin:
      gtin && !FORMATO_GTIN.test(gtin)
        ? 'O GTIN/EAN informado é inválido (use 8, 12, 13 ou 14 dígitos).'
        : undefined,
  };
}

export function paraProdutoRequest(valores: ValoresProduto): ProdutoRequest {
  return {
    nome: valores.nome.trim(),
    descricao: textoOpcional(valores.descricao),
    gtin: textoOpcional(valores.gtin),
  };
}
