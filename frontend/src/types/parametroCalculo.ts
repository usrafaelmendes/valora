/**
 * Contratos de parâmetros de cálculo conforme ParametroCalculoController,
 * ParametroCalculoRequest e ParametroCalculoResponse.
 *
 * As chaves são criadas pelas migrations do backend; o frontend só consulta e altera valores.
 */
export interface ParametroCalculo {
  chave: string;
  descricao: string;
  /** Vazio indica valor livre validado por formato pelo backend (ex.: lista de CFOPs). */
  valoresAceitos: string[];
  /** null quando o parâmetro ainda não foi definido. */
  valor: string | null;
  definido: boolean;
  versao: number;
  atualizadoEm: string;
}

/** Nulo ou em branco volta a "não definido". */
export interface ParametroCalculoRequest {
  valor: string | null;
}
