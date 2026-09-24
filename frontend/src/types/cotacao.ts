/**
 * Contratos de cotações e comparações conforme CotacaoController, ComparacaoController e os
 * DTOs CotacaoRequest, OpcaoCotacaoRequest, CotacaoResponse, CotacaoResumoResponse,
 * OpcaoCotacaoResponse, ComparacaoResponse, ComparacaoResumoResponse e CalculoResponse.
 *
 * Todos os valores (créditos, custo efetivo, posição, empate, situação) são os gravados pelo
 * backend: o frontend apenas os apresenta. Decimais chegam como número JSON; instantes, como
 * ISO-8601; null = não informado ou não obtido (nunca zero presumido).
 */
import type { TipoFornecedor, Uf } from './fornecedor';

/** Componentes de valor da operação (CalculoRequest.ValoresInformados). */
export interface ValoresInformados {
  valorProduto: number | null;
  valorIpi: number | null;
  valorFrete: number | null;
  valorSeguro: number | null;
  valorOutrasDespesas: number | null;
  valorDesconto: number | null;
}

/** Dados fiscais da operação (CalculoRequest.DadosFiscaisInformados); alíquotas em pontos (%). */
export interface DadosFiscaisInformados {
  /** Tag orig da NF-e (0 a 8). */
  origemMercadoria: string | null;
  cfop: string | null;
  aliquotaIcms: number | null;
  aliquotaIpi: number | null;
  aliquotaPis: number | null;
  aliquotaCofins: number | null;
}

export interface OpcaoCotacaoRequest {
  fornecedorId: number | null;
  nfeItemId: number | null;
  condicaoPagamento: string | null;
  observacao: string | null;
  valores: ValoresInformados | null;
  dadosFiscais: DadosFiscaisInformados | null;
}

export interface CotacaoRequest {
  produtoId: number;
  quantidade: number;
  descricao: string | null;
  opcoes: OpcaoCotacaoRequest[];
}

/** Limite de opções por cotação (CotacaoRequest.MAXIMO_OPCOES). */
export const MAXIMO_OPCOES = 50;

export interface CotacaoResumo {
  id: number;
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  descricao: string | null;
  criadoEm: string;
  criadoPorId: number;
}

/** Opção como foi informada na cotação (valores e dados fiscais nulos = não informados). */
export interface OpcaoCotacao {
  id: number;
  fornecedorId: number;
  fornecedorRazaoSocial: string;
  nfeItemId: number | null;
  condicaoPagamento: string | null;
  observacao: string | null;
  valores: ValoresInformados | null;
  dadosFiscais: DadosFiscaisInformados | null;
  criadoEm: string;
}

export interface Cotacao {
  id: number;
  produto: { id: number; nome: string; ativo: boolean };
  quantidade: number;
  descricao: string | null;
  criadoEm: string;
  criadoPorId: number;
  opcoes: OpcaoCotacao[];
  /** false quando há opção incluída depois da última comparação (calculado pelo backend). */
  todasAsOpcoesComparadas: boolean;
  ultimaComparacao: Comparacao | null;
}

export interface ComparacaoResumo {
  id: number;
  cotacaoId: number;
  executadoEm: string;
  executadoPorId: number;
  totalOpcoes: number;
  totalClassificadas: number;
}

/** Situações do enum SituacaoAlternativa do backend. */
export const SITUACOES_ALTERNATIVA = [
  'CLASSIFICADA',
  'CALCULO_INCOMPLETO',
  'FORNECEDOR_DESATIVADO',
  'PRODUTO_DESATIVADO',
  'CFOP_NAO_PARTICIPANTE',
  'CFOPS_PARTICIPANTES_NAO_DEFINIDOS',
  'QUANTIDADE_DIVERGENTE',
] as const;

export type SituacaoAlternativa = (typeof SITUACOES_ALTERNATIVA)[number];

export type StatusCalculo = 'CALCULADO' | 'INCOMPLETO';

export interface ParametroComparacao {
  chave: string;
  /** null = não definido no momento da comparação. */
  valor: string | null;
}

export interface RegraComparacao {
  id: number;
  versao: number;
  nome: string;
  tributo: string;
  prioridade: number;
}

export interface CalculoOperacao {
  nfeItemId: number | null;
  nfeItemDadosFiscaisId: number | null;
  fornecedorId: number | null;
  produtoId: number | null;
  tipoFornecedor: TipoFornecedor | null;
  ufOrigem: Uf | null;
  ufDestino: Uf | null;
  origemMercadoria: string | null;
  cfop: string | null;
  quantidade: number | null;
}

/** Parâmetros vigentes no momento do cálculo (null = não definido naquele momento). */
export interface CalculoParametros {
  fonteValoresOperacao: string | null;
  fonteDadosFiscais: string | null;
  fonteUfOrigem: string | null;
  composicaoValorOperacao: string | null;
  composicaoBaseCreditos: string | null;
  arredondamentoCreditos: string | null;
  criterioArredondamento: string | null;
}

export interface CalculoAliquotas {
  icms: number | null;
  ipi: number | null;
  pis: number | null;
  cofins: number | null;
}

/** Cópia da regra no momento do cálculo. */
export interface CalculoRegra {
  id: number;
  versao: number | null;
  nome: string | null;
  formaAliquota: string | null;
}

export interface CalculoCredito {
  tributo: string;
  situacao: string;
  regra: CalculoRegra | null;
  regrasEmConflito: string | null;
  aliquotaObtida: number | null;
  fator: number | null;
  aliquotaAplicada: number | null;
  baseCalculo: number | null;
  valorSemArredondamento: number | null;
  valor: number | null;
  mensagem: string | null;
}

export interface CalculoPendencia {
  tipo: string;
  bloqueante: boolean;
  mensagem: string;
}

export interface Calculo {
  id: number;
  status: StatusCalculo;
  executadoEm: string;
  executadoPorId: number;
  operacao: CalculoOperacao;
  parametros: CalculoParametros;
  componentes: ValoresInformados;
  aliquotasOperacao: CalculoAliquotas;
  valorOperacao: number | null;
  baseCreditos: number | null;
  creditos: CalculoCredito[];
  totalCreditosSemArredondamento: number | null;
  totalCreditos: number | null;
  diferencaArredondamento: number | null;
  custoEfetivo: number | null;
  pendencias: CalculoPendencia[];
}

export interface Alternativa {
  /** Posição no ranking; null quando não classificada. */
  posicao: number | null;
  /** Mesmo custo efetivo de outra opção classificada. */
  empate: boolean;
  situacao: SituacaoAlternativa;
  motivo: string | null;
  opcaoId: number;
  fornecedor: { id: number; razaoSocial: string; tipo: TipoFornecedor | null; ativo: boolean };
  produtoAtivo: boolean;
  condicaoPagamento: string | null;
  prazoPagamentoBase: string | null;
  quantidade: number | null;
  statusCalculo: StatusCalculo | null;
  valorOperacao: number | null;
  totalCreditos: number | null;
  custoEfetivo: number | null;
  /** Cálculo completo; null quando a opção não foi calculada. */
  calculo: Calculo | null;
}

export interface Comparacao {
  id: number;
  cotacaoId: number;
  executadoEm: string;
  executadoPorId: number;
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  criterioOrdenacao: string;
  totalOpcoes: number;
  totalClassificadas: number;
  configuracao: { parametros: ParametroComparacao[]; regrasAtivas: RegraComparacao[] };
  /** Classificadas, na ordem gravada pelo backend (posição 1 = menor custo efetivo). */
  alternativas: Alternativa[];
  naoClassificadas: Alternativa[];
}
