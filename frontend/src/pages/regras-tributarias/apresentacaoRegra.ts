import { ROTULO_TIPO_FORNECEDOR } from '../../types/fornecedor';
import {
  ROTULO_ABRANGENCIA_UF,
  ROTULO_FORMA_ALIQUOTA,
  type FormaAliquota,
  type RegraTributaria,
} from '../../types/regraTributaria';
import { formatarNumero, formatarPercentual } from '../../utils/formatacao';

/** Nome exibido para um fornecedor ou produto referenciado por id em uma regra. */
export type ResolverNome = (id: number) => string | undefined;

/** Como a regra obtém a alíquota, exatamente como configurado (ex.: "Percentual fixo: 12,34%"). */
export function descreverTaxa(regra: {
  formaAliquota: FormaAliquota;
  aliquota: number | null;
}): string {
  const forma = ROTULO_FORMA_ALIQUOTA[regra.formaAliquota];
  return regra.formaAliquota === 'PERCENTUAL_FIXO' && regra.aliquota !== null
    ? `${forma}: ${formatarPercentual(regra.aliquota)}`
    : forma;
}

export function descreverFator(fator: number | null): string {
  return fator === null ? '—' : formatarNumero(fator);
}

/**
 * Condições preenchidas da regra em texto. Lista vazia = a regra vale para qualquer operação.
 * Referências a cadastros desativados aparecem pelo id, pois só os ativos são listados.
 */
export function descreverCondicoes(
  regra: RegraTributaria,
  nomeFornecedor: ResolverNome,
  nomeProduto: ResolverNome,
): string[] {
  const condicoes: string[] = [];
  if (regra.tipoFornecedor) {
    condicoes.push(`Tipo de fornecedor: ${ROTULO_TIPO_FORNECEDOR[regra.tipoFornecedor]}`);
  }
  if (regra.fornecedorId !== null) {
    condicoes.push(
      `Fornecedor: ${nomeFornecedor(regra.fornecedorId) ?? `#${String(regra.fornecedorId)} (inativo)`}`,
    );
  }
  if (regra.produtoId !== null) {
    condicoes.push(
      `Produto: ${nomeProduto(regra.produtoId) ?? `#${String(regra.produtoId)} (inativo)`}`,
    );
  }
  if (regra.ufOrigem) {
    condicoes.push(`UF de origem: ${regra.ufOrigem}`);
  }
  if (regra.ufDestino) {
    condicoes.push(`UF de destino: ${regra.ufDestino}`);
  }
  if (regra.abrangenciaUf) {
    condicoes.push(`Operação: ${ROTULO_ABRANGENCIA_UF[regra.abrangenciaUf]}`);
  }
  if (regra.origensMercadoria.length > 0) {
    condicoes.push(`Origem da mercadoria: ${regra.origensMercadoria.join(', ')}`);
  }
  if (regra.cfops.length > 0) {
    condicoes.push(`CFOP: ${regra.cfops.join(', ')}`);
  }
  return condicoes;
}
