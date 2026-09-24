import type { TipoFornecedor, Uf } from './fornecedor';

/**
 * Contratos de regras tributárias conforme RegraTributariaController, RegraTributariaRequest,
 * RegraTributariaResponse, RegrasAplicaveisRequest e RegrasAplicaveisResponse.
 *
 * Nenhum valor tributário fica no frontend: alíquotas, fatores e condições vêm do backend
 * e são configurados pelo ADMIN.
 */

/** Mesmos valores do enum Tributo do backend. */
export const TRIBUTOS = ['ICMS', 'IPI', 'PIS', 'COFINS', 'PIS_COFINS'] as const;
export type Tributo = (typeof TRIBUTOS)[number];

export const ROTULO_TRIBUTO: Record<Tributo, string> = {
  ICMS: 'ICMS',
  IPI: 'IPI',
  PIS: 'PIS',
  COFINS: 'COFINS',
  PIS_COFINS: 'PIS/COFINS (combinado)',
};

/** Mesmos valores do enum FormaAliquota do backend. */
export const FORMAS_ALIQUOTA = ['PERCENTUAL_FIXO', 'ALIQUOTA_DA_NFE', 'SEM_CREDITO'] as const;
export type FormaAliquota = (typeof FORMAS_ALIQUOTA)[number];

export const ROTULO_FORMA_ALIQUOTA: Record<FormaAliquota, string> = {
  PERCENTUAL_FIXO: 'Percentual fixo',
  ALIQUOTA_DA_NFE: 'Alíquota da NF-e',
  SEM_CREDITO: 'Sem crédito',
};

export const DESCRICAO_FORMA_ALIQUOTA: Record<FormaAliquota, string> = {
  PERCENTUAL_FIXO: 'Usa a alíquota informada nesta regra.',
  ALIQUOTA_DA_NFE:
    'Usa a alíquota destacada nos dados fiscais da operação, conforme o parâmetro FONTE_DADOS_FISCAIS.',
  SEM_CREDITO: 'Declara crédito zero para as operações atendidas (diferente de não haver regra).',
};

/** Mesmos valores do enum AbrangenciaUf do backend. */
export const ABRANGENCIAS_UF = ['INTERNA', 'INTERESTADUAL'] as const;
export type AbrangenciaUf = (typeof ABRANGENCIAS_UF)[number];

export const ROTULO_ABRANGENCIA_UF: Record<AbrangenciaUf, string> = {
  INTERNA: 'Interna (origem e destino na mesma UF)',
  INTERESTADUAL: 'Interestadual (origem diferente do destino)',
};

export interface RegraTributaria {
  id: number;
  nome: string;
  observacao: string | null;
  tributo: Tributo;
  formaAliquota: FormaAliquota;
  /** Percentual; somente para PERCENTUAL_FIXO. */
  aliquota: number | null;
  /** Multiplicador da alíquota obtida; null para SEM_CREDITO. */
  fator: number | null;
  prioridade: number;
  ativa: boolean;
  tipoFornecedor: TipoFornecedor | null;
  fornecedorId: number | null;
  produtoId: number | null;
  ufOrigem: Uf | null;
  ufDestino: Uf | null;
  abrangenciaUf: AbrangenciaUf | null;
  origensMercadoria: string[];
  cfops: string[];
  /** Incrementada a cada alteração; os cálculos registram id e versão usados. */
  versao: number;
  atualizadoEm: string;
}

/** Condição vazia (null ou lista vazia) = qualquer valor. */
export interface RegraTributariaRequest {
  nome: string;
  observacao: string | null;
  tributo: Tributo;
  formaAliquota: FormaAliquota;
  aliquota: number | null;
  fator: number | null;
  prioridade: number | null;
  ativa: boolean | null;
  tipoFornecedor: TipoFornecedor | null;
  fornecedorId: number | null;
  produtoId: number | null;
  ufOrigem: Uf | null;
  ufDestino: Uf | null;
  abrangenciaUf: AbrangenciaUf | null;
  origensMercadoria: string[];
  cfops: string[];
}

export interface FiltroRegras {
  ativa?: boolean;
  tributo?: Tributo;
}

/** Operação de exemplo; campos ausentes são tratados como desconhecidos. */
export interface RegrasAplicaveisRequest {
  fornecedorId: number | null;
  tipoFornecedor: TipoFornecedor | null;
  produtoId: number | null;
  ufOrigem: Uf | null;
  /** Sem valor, o backend usa o parâmetro UF_DESTINO. */
  ufDestino: Uf | null;
  origemMercadoria: string | null;
  cfop: string | null;
}

/** Mesmos valores de SelecaoTributo.Situacao do backend. */
export type SituacaoSelecao = 'APLICAVEL' | 'SEM_REGRA' | 'CONFLITO';

export interface RegraSelecionada {
  id: number;
  nome: string;
  versao: number;
  tributo: Tributo;
  formaAliquota: FormaAliquota;
  aliquota: number | null;
  fator: number | null;
  prioridade: number;
}

export interface SelecaoTributo {
  tributo: Tributo;
  situacao: SituacaoSelecao;
  /** Explicação do backend para SEM_REGRA e CONFLITO; null quando APLICAVEL. */
  mensagem: string | null;
  /** APLICAVEL: a regra escolhida; CONFLITO: as regras empatadas; SEM_REGRA: vazia. */
  regras: RegraSelecionada[];
}

export interface RegrasAplicaveisResponse {
  /** Operação considerada pelo backend (ex.: com a UF de destino do parâmetro). */
  operacao: RegrasAplicaveisRequest;
  tributos: SelecaoTributo[];
}
