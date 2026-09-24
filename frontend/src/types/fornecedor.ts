/** Contratos de fornecedores conforme FornecedorController, FornecedorRequest e FornecedorResponse. */

/** Mesmos valores do enum Uf do backend. */
export const UFS = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
] as const;

export type Uf = (typeof UFS)[number];

/** Mesmos valores do enum TipoFornecedor do backend. */
export const TIPOS_FORNECEDOR = ['FABRICANTE', 'ATACADISTA'] as const;

export type TipoFornecedor = (typeof TIPOS_FORNECEDOR)[number];

export const ROTULO_TIPO_FORNECEDOR: Record<TipoFornecedor, string> = {
  FABRICANTE: 'Fabricante',
  ATACADISTA: 'Atacadista/revendedor',
};

export interface Fornecedor {
  id: number;
  razaoSocial: string;
  /** Sem máscara (14 caracteres); a formatação para exibição é feita no frontend. */
  cnpj: string;
  /** UF de emissão da NF-e. */
  uf: Uf;
  tipo: TipoFornecedor;
  prazoPagamentoBase: string | null;
  ativo: boolean;
}

export interface FornecedorRequest {
  razaoSocial: string;
  cnpj: string;
  uf: Uf;
  tipo: TipoFornecedor;
  prazoPagamentoBase: string | null;
}
