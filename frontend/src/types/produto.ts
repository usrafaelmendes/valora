/** Contratos de produtos conforme ProdutoController, ProdutoRequest e ProdutoResponse. */

export interface Produto {
  id: number;
  nome: string;
  descricao: string | null;
  /** GTIN/EAN (8, 12, 13 ou 14 dígitos); opcional. */
  gtin: string | null;
  ativo: boolean;
}

export interface ProdutoRequest {
  nome: string;
  descricao: string | null;
  gtin: string | null;
}
