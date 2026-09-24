import { excedeLimite, textoOpcional, type ErrosFormulario } from '../../hooks/useFormulario';
import {
  TIPOS_FORNECEDOR,
  UFS,
  type Fornecedor,
  type FornecedorRequest,
  type TipoFornecedor,
  type Uf,
} from '../../types/fornecedor';
import { formatarCnpj, normalizarCnpj } from '../../utils/formatacao';

/** Valores do formulário de fornecedor, como digitados ('' = não selecionado). */
export interface ValoresFornecedor extends Record<string, string> {
  razaoSocial: string;
  cnpj: string;
  uf: Uf | '';
  tipo: TipoFornecedor | '';
  prazoPagamentoBase: string;
}

export const CAMPOS_FORNECEDOR: string[] = [
  'razaoSocial',
  'cnpj',
  'uf',
  'tipo',
  'prazoPagamentoBase',
];

/**
 * Mesmo formato aceito pelo backend (numérico ou alfanumérico, 2 dígitos verificadores
 * numéricos). Os dígitos verificadores são conferidos pela API.
 */
const FORMATO_CNPJ = /^[0-9A-Z]{12}[0-9]{2}$/;

export function ehUf(valor: string | null): valor is Uf {
  return UFS.some((uf) => uf === valor);
}

export function ehTipoFornecedor(valor: string | null): valor is TipoFornecedor {
  return TIPOS_FORNECEDOR.some((tipo) => tipo === valor);
}

export function valoresIniciaisFornecedor(fornecedor: Fornecedor | null): ValoresFornecedor {
  return {
    razaoSocial: fornecedor?.razaoSocial ?? '',
    cnpj: fornecedor ? formatarCnpj(fornecedor.cnpj) : '',
    uf: fornecedor?.uf ?? '',
    tipo: fornecedor?.tipo ?? '',
    prazoPagamentoBase: fornecedor?.prazoPagamentoBase ?? '',
  };
}

/** Espelha as restrições de FornecedorRequest, com as mesmas mensagens do backend. */
export function validarFornecedor(valores: ValoresFornecedor): ErrosFormulario<ValoresFornecedor> {
  const cnpj = normalizarCnpj(valores.cnpj);
  return {
    razaoSocial: !valores.razaoSocial.trim()
      ? 'A razão social é obrigatória.'
      : excedeLimite(valores.razaoSocial, 150, 'A razão social deve ter no máximo 150 caracteres.'),
    cnpj: !cnpj
      ? 'O CNPJ é obrigatório.'
      : FORMATO_CNPJ.test(cnpj)
        ? undefined
        : 'O CNPJ informado é inválido.',
    uf: valores.uf ? undefined : 'A UF de emissão é obrigatória.',
    tipo: valores.tipo ? undefined : 'O tipo do fornecedor é obrigatório.',
    prazoPagamentoBase: excedeLimite(
      valores.prazoPagamentoBase,
      100,
      'O prazo de pagamento base deve ter no máximo 100 caracteres.',
    ),
  };
}

/** Converte os valores já validados; o CNPJ é enviado sem máscara. */
export function paraFornecedorRequest(valores: ValoresFornecedor): FornecedorRequest {
  if (!valores.uf || !valores.tipo) {
    throw new Error('Formulário de fornecedor enviado sem validação.');
  }
  return {
    razaoSocial: valores.razaoSocial.trim(),
    cnpj: normalizarCnpj(valores.cnpj),
    uf: valores.uf,
    tipo: valores.tipo,
    prazoPagamentoBase: textoOpcional(valores.prazoPagamentoBase),
  };
}
