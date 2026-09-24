/**
 * Corpo de erro da API no formato ProblemDetail (RFC 9457), como produzido pelo
 * GlobalExceptionHandler e pelo SecurityErrorHandler do backend.
 */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Erros de validação por campo (propriedade "erros" do backend). */
  erros?: Record<string, string>;
}
