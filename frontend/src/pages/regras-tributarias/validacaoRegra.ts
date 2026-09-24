import { excedeLimite, textoOpcional, type ErrosFormulario } from '../../hooks/useFormulario';
import type { TipoFornecedor, Uf } from '../../types/fornecedor';
import {
  ABRANGENCIAS_UF,
  FORMAS_ALIQUOTA,
  TRIBUTOS,
  type AbrangenciaUf,
  type FormaAliquota,
  type RegraTributaria,
  type RegraTributariaRequest,
  type Tributo,
} from '../../types/regraTributaria';

/**
 * Valores do formulário de regra tributária, como digitados ('' = não selecionado).
 * Listas (origens e CFOPs) ficam separadas por vírgula; "ativa" é 'true' ou 'false'.
 *
 * A validação local espelha as restrições de RegraTributariaRequest; as regras de coerência
 * entre campos (ex.: abrangência x UFs) ficam no backend, que devolve o erro no campo.
 */
export interface ValoresRegra extends Record<string, string> {
  nome: string;
  observacao: string;
  tributo: Tributo | '';
  formaAliquota: FormaAliquota | '';
  aliquota: string;
  fator: string;
  prioridade: string;
  ativa: 'true' | 'false';
  tipoFornecedor: TipoFornecedor | '';
  fornecedorId: string;
  produtoId: string;
  ufOrigem: Uf | '';
  ufDestino: Uf | '';
  abrangenciaUf: AbrangenciaUf | '';
  origensMercadoria: string;
  cfops: string;
}

export const CAMPOS_REGRA: string[] = [
  'nome',
  'observacao',
  'tributo',
  'formaAliquota',
  'aliquota',
  'fator',
  'prioridade',
  'ativa',
  'tipoFornecedor',
  'fornecedorId',
  'produtoId',
  'ufOrigem',
  'ufDestino',
  'abrangenciaUf',
  'origensMercadoria',
  'cfops',
];

export function ehTributo(valor: string | null): valor is Tributo {
  return TRIBUTOS.some((tributo) => tributo === valor);
}

export function ehFormaAliquota(valor: string | null): valor is FormaAliquota {
  return FORMAS_ALIQUOTA.some((forma) => forma === valor);
}

export function ehAbrangenciaUf(valor: string | null): valor is AbrangenciaUf {
  return ABRANGENCIAS_UF.some((abrangencia) => abrangencia === valor);
}

/** Lista guardada no formulário como texto separado por vírgula. */
export function paraLista(valor: string): string[] {
  return valor
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export function deLista(itens: string[]): string {
  return itens.join(',');
}

/** Decimal exibido com vírgula, como digitado no Brasil. */
function decimalParaTexto(valor: number | null): string {
  return valor === null ? '' : String(valor).replace('.', ',');
}

/** Aceita vírgula ou ponto como separador decimal. */
function normalizarDecimal(valor: string): string {
  return valor.trim().replace(',', '.');
}

export function valoresIniciaisRegra(regra: RegraTributaria | null): ValoresRegra {
  return {
    nome: regra?.nome ?? '',
    observacao: regra?.observacao ?? '',
    tributo: regra?.tributo ?? '',
    formaAliquota: regra?.formaAliquota ?? '',
    aliquota: decimalParaTexto(regra?.aliquota ?? null),
    fator: decimalParaTexto(regra?.fator ?? null),
    prioridade: regra ? String(regra.prioridade) : '',
    ativa: regra && !regra.ativa ? 'false' : 'true',
    tipoFornecedor: regra?.tipoFornecedor ?? '',
    fornecedorId: regra?.fornecedorId ? String(regra.fornecedorId) : '',
    produtoId: regra?.produtoId ? String(regra.produtoId) : '',
    ufOrigem: regra?.ufOrigem ?? '',
    ufDestino: regra?.ufDestino ?? '',
    abrangenciaUf: regra?.abrangenciaUf ?? '',
    origensMercadoria: deLista(regra?.origensMercadoria ?? []),
    cfops: deLista(regra?.cfops ?? []),
  };
}

/** A alíquota só existe para PERCENTUAL_FIXO; o fator não existe para SEM_CREDITO. */
export function usaAliquota(forma: FormaAliquota | ''): boolean {
  return forma === 'PERCENTUAL_FIXO';
}

export function usaFator(forma: FormaAliquota | ''): boolean {
  return forma !== 'SEM_CREDITO';
}

const FORMATO_ALIQUOTA = /^\d{1,3}(\.\d{1,4})?$/;
const FORMATO_FATOR = /^\d(\.\d{1,4})?$/;
const FORMATO_PRIORIDADE = /^\d{1,4}$/;
const FORMATO_CFOP = /^\d{4}$/;

function validarAliquota(valores: ValoresRegra): string | undefined {
  if (!usaAliquota(valores.formaAliquota)) {
    return undefined;
  }
  const aliquota = normalizarDecimal(valores.aliquota);
  if (!aliquota) {
    return 'A alíquota é obrigatória para PERCENTUAL_FIXO.';
  }
  if (!/^\d+(\.\d+)?$/.test(aliquota) || Number(aliquota) > 100) {
    return 'A alíquota deve estar entre 0 e 100.';
  }
  return FORMATO_ALIQUOTA.test(aliquota)
    ? undefined
    : 'A alíquota aceita no máximo 4 casas decimais.';
}

function validarFator(valores: ValoresRegra): string | undefined {
  const fator = normalizarDecimal(valores.fator);
  if (!usaFator(valores.formaAliquota) || !fator) {
    return undefined;
  }
  if (!/^\d+(\.\d+)?$/.test(fator) || Number(fator) <= 0) {
    return 'O fator deve ser maior que zero.';
  }
  return FORMATO_FATOR.test(fator)
    ? undefined
    : 'O fator deve ser menor que 10 e ter no máximo 4 casas decimais.';
}

function validarPrioridade(valor: string): string | undefined {
  const prioridade = valor.trim();
  if (!prioridade) {
    return undefined;
  }
  return FORMATO_PRIORIDADE.test(prioridade) && Number(prioridade) <= 1000
    ? undefined
    : 'A prioridade deve estar entre 0 e 1000.';
}

function validarCfops(valor: string): string | undefined {
  const cfops = paraLista(valor);
  if (cfops.some((cfop) => !FORMATO_CFOP.test(cfop))) {
    return 'Cada CFOP deve ter 4 dígitos.';
  }
  return cfops.length > 100 ? 'Informe no máximo 100 CFOPs.' : undefined;
}

export function validarRegra(valores: ValoresRegra): ErrosFormulario<ValoresRegra> {
  return {
    nome: !valores.nome.trim()
      ? 'O nome é obrigatório.'
      : excedeLimite(valores.nome, 150, 'O nome deve ter no máximo 150 caracteres.'),
    observacao: excedeLimite(
      valores.observacao,
      1000,
      'A observação deve ter no máximo 1000 caracteres.',
    ),
    tributo: valores.tributo ? undefined : 'O tributo é obrigatório.',
    formaAliquota: valores.formaAliquota
      ? undefined
      : 'A forma de obtenção da alíquota é obrigatória.',
    aliquota: validarAliquota(valores),
    fator: validarFator(valores),
    prioridade: validarPrioridade(valores.prioridade),
    cfops: validarCfops(valores.cfops),
  };
}

function idOpcional(valor: string): number | null {
  return valor ? Number(valor) : null;
}

function decimalOpcional(valor: string): number | null {
  const normalizado = normalizarDecimal(valor);
  return normalizado ? Number(normalizado) : null;
}

/**
 * Converte os valores já validados. Campos que não se aplicam à forma escolhida são enviados
 * vazios; fator e prioridade vazios ficam a cargo do backend (fator 1 e prioridade 0).
 */
export function paraRegraRequest(valores: ValoresRegra): RegraTributariaRequest {
  if (!valores.tributo || !valores.formaAliquota) {
    throw new Error('Formulário de regra enviado sem validação.');
  }
  const prioridade = valores.prioridade.trim();
  return {
    nome: valores.nome.trim(),
    observacao: textoOpcional(valores.observacao),
    tributo: valores.tributo,
    formaAliquota: valores.formaAliquota,
    aliquota: usaAliquota(valores.formaAliquota) ? decimalOpcional(valores.aliquota) : null,
    fator: usaFator(valores.formaAliquota) ? decimalOpcional(valores.fator) : null,
    prioridade: prioridade ? Number(prioridade) : null,
    ativa: valores.ativa === 'true',
    tipoFornecedor: valores.tipoFornecedor || null,
    fornecedorId: idOpcional(valores.fornecedorId),
    produtoId: idOpcional(valores.produtoId),
    ufOrigem: valores.ufOrigem || null,
    ufDestino: valores.ufDestino || null,
    abrangenciaUf: valores.abrangenciaUf || null,
    origensMercadoria: paraLista(valores.origensMercadoria),
    cfops: paraLista(valores.cfops),
  };
}
