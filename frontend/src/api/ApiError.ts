import type { ProblemDetail } from '../types/api';

/** Status usado quando não houve resposta HTTP (backend fora do ar, rede indisponível). */
export const STATUS_SEM_RESPOSTA = 0;

const MENSAGENS_PADRAO: Record<number, string> = {
  [STATUS_SEM_RESPOSTA]:
    'Não foi possível conectar ao servidor. Verifique a conexão e tente novamente.',
  400: 'Os dados enviados são inválidos.',
  401: 'Sua sessão expirou ou não é válida. Entre novamente.',
  403: 'Você não tem permissão para acessar este recurso.',
  404: 'O recurso solicitado não foi encontrado.',
  409: 'A operação conflita com dados já cadastrados.',
  413: 'O arquivo enviado excede o tamanho máximo permitido.',
  415: 'O tipo do conteúdo enviado não é aceito.',
  422: 'Os dados enviados não puderam ser processados.',
};

const MENSAGEM_GENERICA = 'Ocorreu um erro inesperado. Tente novamente ou contate o administrador.';

/**
 * Erro padronizado de qualquer chamada à API. A mensagem vem do "detail" do backend
 * quando existir; caso contrário, usa um texto padrão para o status.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail | null;
  /** Mensagens de validação por campo, quando o backend as informar. */
  readonly erros: Record<string, string>;

  constructor(status: number, problem: ProblemDetail | null = null) {
    super(problem?.detail ?? MENSAGENS_PADRAO[status] ?? MENSAGEM_GENERICA);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
    this.erros = problem?.erros ?? {};
  }

  get semResposta(): boolean {
    return this.status === STATUS_SEM_RESPOSTA;
  }
}

/** Mensagem segura para exibir ao usuário, qualquer que seja o erro capturado. */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    return erro.message;
  }
  return MENSAGEM_GENERICA;
}
