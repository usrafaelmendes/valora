import { excedeLimite, textoOpcional, type ErrosFormulario } from '../../hooks/useFormulario';
import type {
  CotacaoRequest,
  DadosFiscaisInformados,
  OpcaoCotacaoRequest,
  ValoresInformados,
} from '../../types/cotacao';

/**
 * Formulários de cotação e de opção. A validação local espelha as restrições de
 * CotacaoRequest, OpcaoCotacaoRequest e CalculoRequest (mesmas mensagens); as regras que
 * dependem do banco (fornecedor ativo, item de NF-e do produto etc.) ficam no backend.
 *
 * Os campos da opção usam os mesmos nomes dos erros devolvidos pela API (ex.:
 * "valores.valorProduto"), para que cada mensagem apareça no próprio campo.
 */

export const CAMPOS_VALORES = [
  'valores.valorProduto',
  'valores.valorIpi',
  'valores.valorFrete',
  'valores.valorSeguro',
  'valores.valorOutrasDespesas',
  'valores.valorDesconto',
] as const;

export const CAMPOS_ALIQUOTAS = [
  'dadosFiscais.aliquotaIcms',
  'dadosFiscais.aliquotaIpi',
  'dadosFiscais.aliquotaPis',
  'dadosFiscais.aliquotaCofins',
] as const;

export type CampoValor = (typeof CAMPOS_VALORES)[number];
export type CampoAliquota = (typeof CAMPOS_ALIQUOTAS)[number];

export const ROTULO_CAMPO_OPCAO: Record<CampoValor | CampoAliquota, string> = {
  'valores.valorProduto': 'Valor dos produtos',
  'valores.valorIpi': 'IPI',
  'valores.valorFrete': 'Frete',
  'valores.valorSeguro': 'Seguro',
  'valores.valorOutrasDespesas': 'Outras despesas',
  'valores.valorDesconto': 'Desconto',
  'dadosFiscais.aliquotaIcms': 'Alíquota de ICMS (%)',
  'dadosFiscais.aliquotaIpi': 'Alíquota de IPI (%)',
  'dadosFiscais.aliquotaPis': 'Alíquota de PIS (%)',
  'dadosFiscais.aliquotaCofins': 'Alíquota de COFINS (%)',
};

/** Valores de uma opção, como digitados ('' = não informado). */
export interface ValoresOpcao extends Record<string, string> {
  fornecedorId: string;
  nfeItemId: string;
  condicaoPagamento: string;
  observacao: string;
  'valores.valorProduto': string;
  'valores.valorIpi': string;
  'valores.valorFrete': string;
  'valores.valorSeguro': string;
  'valores.valorOutrasDespesas': string;
  'valores.valorDesconto': string;
  'dadosFiscais.origemMercadoria': string;
  'dadosFiscais.cfop': string;
  'dadosFiscais.aliquotaIcms': string;
  'dadosFiscais.aliquotaIpi': string;
  'dadosFiscais.aliquotaPis': string;
  'dadosFiscais.aliquotaCofins': string;
}

export function valoresIniciaisOpcao(): ValoresOpcao {
  return {
    fornecedorId: '',
    nfeItemId: '',
    condicaoPagamento: '',
    observacao: '',
    'valores.valorProduto': '',
    'valores.valorIpi': '',
    'valores.valorFrete': '',
    'valores.valorSeguro': '',
    'valores.valorOutrasDespesas': '',
    'valores.valorDesconto': '',
    'dadosFiscais.origemMercadoria': '',
    'dadosFiscais.cfop': '',
    'dadosFiscais.aliquotaIcms': '',
    'dadosFiscais.aliquotaIpi': '',
    'dadosFiscais.aliquotaPis': '',
    'dadosFiscais.aliquotaCofins': '',
  };
}

export const CAMPOS_OPCAO: string[] = Object.keys(valoresIniciaisOpcao());

/** Aceita vírgula ou ponto como separador decimal. */
function normalizarDecimal(valor: string): string {
  return valor.trim().replace(',', '.');
}

const NUMERO = /^\d+(\.\d+)?$/;

/** Até 13 dígitos inteiros e 2 decimais, não negativo (ValoresInformados). */
function validarValor(valor: string): string | undefined {
  const numero = normalizarDecimal(valor);
  if (!numero) {
    return undefined;
  }
  if (!NUMERO.test(numero)) {
    return 'O valor não pode ser negativo.';
  }
  return /^\d{1,13}(\.\d{1,2})?$/.test(numero)
    ? undefined
    : 'Use no máximo 13 dígitos inteiros e 2 decimais.';
}

/** Entre 0 e 100, com até 4 casas decimais (DadosFiscaisInformados). */
function validarAliquota(valor: string): string | undefined {
  const numero = normalizarDecimal(valor);
  if (!numero) {
    return undefined;
  }
  if (!NUMERO.test(numero) || Number(numero) > 100) {
    return 'A alíquota deve estar entre 0 e 100.';
  }
  return /^\d{1,3}(\.\d{1,4})?$/.test(numero)
    ? undefined
    : 'A alíquota aceita no máximo 4 casas decimais.';
}

export function validarOpcao(valores: ValoresOpcao): ErrosFormulario<ValoresOpcao> {
  const erros: ErrosFormulario<ValoresOpcao> = {
    fornecedorId:
      valores.fornecedorId || valores.nfeItemId
        ? undefined
        : 'Informe o fornecedor ou o item de NF-e da opção.',
    condicaoPagamento: excedeLimite(
      valores.condicaoPagamento,
      100,
      'A condição de pagamento deve ter no máximo 100 caracteres.',
    ),
    observacao: excedeLimite(
      valores.observacao,
      500,
      'A observação deve ter no máximo 500 caracteres.',
    ),
    'dadosFiscais.cfop':
      valores['dadosFiscais.cfop'].trim() && !/^\d{4}$/.test(valores['dadosFiscais.cfop'].trim())
        ? 'O CFOP deve ter 4 dígitos.'
        : undefined,
  };
  for (const campo of CAMPOS_VALORES) {
    erros[campo] = validarValor(valores[campo]);
  }
  for (const campo of CAMPOS_ALIQUOTAS) {
    erros[campo] = validarAliquota(valores[campo]);
  }
  return erros;
}

export function semErros<T>(erros: ErrosFormulario<T>): boolean {
  return Object.values(erros).every((mensagem) => !mensagem);
}

function decimalOpcional(valor: string): number | null {
  const normalizado = normalizarDecimal(valor);
  return normalizado ? Number(normalizado) : null;
}

function idOpcional(valor: string): number | null {
  return valor ? Number(valor) : null;
}

/**
 * Componente em branco é enviado como null: para o backend, ele fica indisponível (não é
 * zero). Sem nenhum componente, o grupo inteiro é enviado como não informado.
 */
function paraValores(valores: ValoresOpcao): ValoresInformados | null {
  const grupo: ValoresInformados = {
    valorProduto: decimalOpcional(valores['valores.valorProduto']),
    valorIpi: decimalOpcional(valores['valores.valorIpi']),
    valorFrete: decimalOpcional(valores['valores.valorFrete']),
    valorSeguro: decimalOpcional(valores['valores.valorSeguro']),
    valorOutrasDespesas: decimalOpcional(valores['valores.valorOutrasDespesas']),
    valorDesconto: decimalOpcional(valores['valores.valorDesconto']),
  };
  return Object.values(grupo).every((valor) => valor === null) ? null : grupo;
}

function paraDadosFiscais(valores: ValoresOpcao): DadosFiscaisInformados | null {
  const grupo: DadosFiscaisInformados = {
    origemMercadoria: valores['dadosFiscais.origemMercadoria'] || null,
    cfop: valores['dadosFiscais.cfop'].trim() || null,
    aliquotaIcms: decimalOpcional(valores['dadosFiscais.aliquotaIcms']),
    aliquotaIpi: decimalOpcional(valores['dadosFiscais.aliquotaIpi']),
    aliquotaPis: decimalOpcional(valores['dadosFiscais.aliquotaPis']),
    aliquotaCofins: decimalOpcional(valores['dadosFiscais.aliquotaCofins']),
  };
  return Object.values(grupo).every((valor) => valor === null) ? null : grupo;
}

export function paraOpcaoRequest(valores: ValoresOpcao): OpcaoCotacaoRequest {
  return {
    fornecedorId: idOpcional(valores.fornecedorId),
    nfeItemId: idOpcional(valores.nfeItemId),
    condicaoPagamento: textoOpcional(valores.condicaoPagamento),
    observacao: textoOpcional(valores.observacao),
    valores: paraValores(valores),
    dadosFiscais: paraDadosFiscais(valores),
  };
}

/**
 * Erros da API de uma opção dentro da cotação (ex.: "opcoes[1].valores.valorProduto"),
 * com o prefixo removido para corresponder aos campos do formulário da opção.
 */
export function errosDaOpcao(
  erros: Record<string, string>,
  indice: number,
): ErrosFormulario<ValoresOpcao> {
  const prefixo = `opcoes[${String(indice)}].`;
  const daOpcao: ErrosFormulario<ValoresOpcao> = {};
  for (const [campo, mensagem] of Object.entries(erros)) {
    const semPrefixo = campo.slice(prefixo.length);
    if (campo.startsWith(prefixo) && CAMPOS_OPCAO.includes(semPrefixo)) {
      daOpcao[semPrefixo] = mensagem;
    }
  }
  return daOpcao;
}

/** Campos das opções com erro devolvido pela API que aparecem no próprio formulário. */
export function camposDasOpcoes(total: number): string[] {
  return Array.from({ length: total }, (_, indice) =>
    CAMPOS_OPCAO.map((campo) => `opcoes[${String(indice)}].${campo}`),
  ).flat();
}

/** Dados gerais da cotação, como digitados. */
export interface ValoresCotacao extends Record<string, string> {
  produtoId: string;
  quantidade: string;
  descricao: string;
}

export const VALORES_INICIAIS_COTACAO: ValoresCotacao = {
  produtoId: '',
  quantidade: '',
  descricao: '',
};

export const CAMPOS_COTACAO: string[] = Object.keys(VALORES_INICIAIS_COTACAO);

function validarQuantidade(valor: string): string | undefined {
  const quantidade = normalizarDecimal(valor);
  if (!quantidade) {
    return 'A quantidade é obrigatória.';
  }
  if (!NUMERO.test(quantidade) || Number(quantidade) <= 0) {
    return 'A quantidade deve ser maior que zero.';
  }
  return /^\d{1,11}(\.\d{1,4})?$/.test(quantidade)
    ? undefined
    : 'A quantidade aceita até 11 dígitos inteiros e 4 decimais.';
}

export function validarCotacao(valores: ValoresCotacao): ErrosFormulario<ValoresCotacao> {
  return {
    produtoId: valores.produtoId ? undefined : 'O produto é obrigatório.',
    quantidade: validarQuantidade(valores.quantidade),
    descricao: excedeLimite(
      valores.descricao,
      500,
      'A descrição deve ter no máximo 500 caracteres.',
    ),
  };
}

export function paraCotacaoRequest(
  valores: ValoresCotacao,
  opcoes: ValoresOpcao[],
): CotacaoRequest {
  return {
    produtoId: Number(valores.produtoId),
    quantidade: Number(normalizarDecimal(valores.quantidade)),
    descricao: textoOpcional(valores.descricao),
    opcoes: opcoes.map(paraOpcaoRequest),
  };
}
