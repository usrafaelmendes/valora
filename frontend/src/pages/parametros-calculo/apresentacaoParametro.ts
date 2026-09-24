/**
 * Apresentação dos parâmetros de cálculo. Somente rótulos e o tipo de campo de edição:
 * as chaves, os valores aceitos e a validação vêm do backend (ChaveParametro).
 */

/** Como o valor é editado; chaves desconhecidas usam texto livre validado pelo backend. */
export type FormatoParametro = 'VALOR_UNICO' | 'LISTA_VALORES' | 'LISTA_CFOP' | 'TEXTO';

/** Formato das chaves existentes no backend (enum ChaveParametro.Formato). */
const FORMATOS: Record<string, FormatoParametro> = {
  UF_DESTINO: 'VALOR_UNICO',
  FONTE_UF_ORIGEM: 'VALOR_UNICO',
  ARREDONDAMENTO_CREDITOS: 'VALOR_UNICO',
  CFOPS_PARTICIPANTES: 'LISTA_CFOP',
  FONTE_VALORES_OPERACAO: 'VALOR_UNICO',
  FONTE_DADOS_FISCAIS: 'VALOR_UNICO',
  COMPOSICAO_VALOR_OPERACAO: 'LISTA_VALORES',
  COMPOSICAO_BASE_CREDITOS: 'LISTA_VALORES',
  CRITERIO_ARREDONDAMENTO: 'VALOR_UNICO',
};

export function formatoDoParametro(chave: string, valoresAceitos: string[]): FormatoParametro {
  const conhecido = FORMATOS[chave];
  if (conhecido) {
    return conhecido;
  }
  return valoresAceitos.length > 0 ? 'VALOR_UNICO' : 'TEXTO';
}

const TITULOS: Record<string, string> = {
  UF_DESTINO: 'UF de destino',
  FONTE_UF_ORIGEM: 'Fonte da UF de origem',
  ARREDONDAMENTO_CREDITOS: 'Momento do arredondamento dos créditos',
  CFOPS_PARTICIPANTES: 'CFOPs participantes',
  FONTE_VALORES_OPERACAO: 'Fonte dos valores da operação',
  FONTE_DADOS_FISCAIS: 'Fonte dos dados fiscais',
  COMPOSICAO_VALOR_OPERACAO: 'Composição do valor da operação',
  COMPOSICAO_BASE_CREDITOS: 'Composição da base dos créditos',
  CRITERIO_ARREDONDAMENTO: 'Critério de arredondamento',
};

export function tituloDoParametro(chave: string): string {
  return TITULOS[chave] ?? chave;
}

/** Rótulos dos valores aceitos pelo backend; valores sem rótulo aparecem como o código. */
const ROTULOS_VALOR: Record<string, string> = {
  EMITENTE_NFE: 'UF do emitente da NF-e',
  CADASTRO_FORNECEDOR: 'UF do cadastro do fornecedor',
  POR_CREDITO: 'Arredondar cada crédito antes de somar',
  SOMENTE_TOTAL: 'Arredondar somente o total',
  NFE_ITEM: 'Item da NF-e',
  INFORMADO: 'Valores informados no cálculo (ex.: cotação)',
  ULTIMA_NFE_FORNECEDOR_PRODUTO: 'Última NF-e do mesmo fornecedor e produto',
  VALOR_PRODUTO: 'Valor dos produtos',
  IPI: 'IPI',
  FRETE: 'Frete',
  SEGURO: 'Seguro',
  OUTRAS_DESPESAS: 'Outras despesas',
  DESCONTO: 'Desconto (subtraído)',
  VALOR_OPERACAO: 'O próprio valor da operação',
  MEIO_PARA_CIMA: 'Meio para cima (0,125 → 0,13)',
  MEIO_PARA_PAR: 'Meio para o par (0,125 → 0,12)',
};

export function rotuloDoValor(valor: string): string {
  return ROTULOS_VALOR[valor] ?? valor;
}

/** Valor gravado (listas separadas por vírgula) em texto legível. */
export function descreverValor(valor: string): string {
  return valor
    .split(',')
    .map((parte) => rotuloDoValor(parte.trim()))
    .join(', ');
}
