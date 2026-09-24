import { API_BASE_URL } from '../config/env';
import type { ProblemDetail } from '../types/api';
import { ApiError, STATUS_SEM_RESPOSTA } from './ApiError';

/**
 * Cliente HTTP central da aplicação, baseado em fetch.
 *
 * - adiciona `Authorization: Bearer <token>` quando há sessão;
 * - converte respostas de erro (ProblemDetail) em ApiError;
 * - avisa a camada de autenticação quando uma chamada autenticada recebe 401;
 * - aceita JSON, multipart/form-data (upload) e download binário.
 *
 * Não contém regra de negócio nem cálculo: apenas transporta dados da API.
 */

type MetodoHttp = 'GET' | 'POST' | 'PUT' | 'DELETE';

export interface ConfiguracaoHttp {
  /** Token da sessão atual, ou null quando não há sessão. */
  obterToken: () => string | null;
  /** Chamado quando uma requisição autenticada recebe 401 (token expirado ou inválido). */
  aoNaoAutorizado: () => void;
}

const CONFIGURACAO_PADRAO: ConfiguracaoHttp = {
  obterToken: () => null,
  aoNaoAutorizado: () => undefined,
};

let configuracao: ConfiguracaoHttp = CONFIGURACAO_PADRAO;

/** Registra de onde vem o token e o que fazer diante de 401. Retorna a função que desfaz o registro. */
export function configurarHttpClient(nova: ConfiguracaoHttp): () => void {
  configuracao = nova;
  return () => {
    if (configuracao === nova) {
      configuracao = CONFIGURACAO_PADRAO;
    }
  };
}

export interface OpcoesRequisicao {
  /**
   * `false` para rotas públicas (ex.: login): não envia o token e um 401 não é tratado
   * como sessão expirada. Padrão: `true`.
   */
  autenticada?: boolean;
  signal?: AbortSignal;
}

interface Envio extends OpcoesRequisicao {
  metodo: MetodoHttp;
  caminho: string;
  json?: unknown;
  formData?: FormData;
  aceita?: string;
}

async function enviar({
  metodo,
  caminho,
  json,
  formData,
  aceita,
  autenticada = true,
  signal,
}: Envio): Promise<Response> {
  const headers = new Headers();
  if (aceita) {
    headers.set('Accept', aceita);
  }

  let body: BodyInit | undefined;
  if (formData) {
    // Sem Content-Type: o navegador define multipart/form-data com o boundary correto.
    body = formData;
  } else if (json !== undefined) {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(json);
  }

  const token = autenticada ? configuracao.obterToken() : null;
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let resposta: Response;
  try {
    resposta = await fetch(`${API_BASE_URL}${caminho}`, { method: metodo, headers, body, signal });
  } catch (erro) {
    // Cancelamento pedido pelo chamador (ex.: TanStack Query) não é falha de rede.
    if (erro instanceof DOMException && erro.name === 'AbortError') {
      throw erro;
    }
    throw new ApiError(STATUS_SEM_RESPOSTA);
  }

  if (!resposta.ok) {
    const problem = await lerProblemDetail(resposta);
    if (resposta.status === 401 && autenticada) {
      configuracao.aoNaoAutorizado();
    }
    throw new ApiError(resposta.status, problem);
  }
  return resposta;
}

async function lerProblemDetail(resposta: Response): Promise<ProblemDetail | null> {
  const tipo = resposta.headers.get('Content-Type') ?? '';
  if (!tipo.includes('json')) {
    return null;
  }
  try {
    const corpo: unknown = await resposta.json();
    return typeof corpo === 'object' && corpo !== null ? corpo : null;
  } catch {
    return null;
  }
}

/** Lê o corpo JSON da resposta; respostas sem corpo (ex.: 204) resultam em undefined. */
async function lerJson<T>(resposta: Response): Promise<T> {
  const texto = await resposta.text();
  if (!texto) {
    return undefined as T;
  }
  // Os tipos dos DTOs espelham os contratos do backend; não há validação em tempo de execução.
  const dados: unknown = JSON.parse(texto);
  return dados as T;
}

const ACEITA_JSON = 'application/json, application/problem+json';

async function requisitarJson<T>(envio: Envio): Promise<T> {
  const resposta = await enviar({ aceita: ACEITA_JSON, ...envio });
  return lerJson<T>(resposta);
}

export interface ArquivoBaixado {
  blob: Blob;
  /** Nome sugerido pelo backend em Content-Disposition, quando informado. */
  nomeArquivo: string | null;
}

/** Extrai o nome do arquivo de um cabeçalho Content-Disposition (filename* tem prioridade). */
export function nomeDoContentDisposition(cabecalho: string | null): string | null {
  if (!cabecalho) {
    return null;
  }
  const codificado = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/.exec(cabecalho);
  if (codificado?.[1]) {
    try {
      return decodeURIComponent(codificado[1].trim());
    } catch {
      // Valor mal codificado: tenta o parâmetro "filename" simples.
    }
  }
  const simples = /filename\s*=\s*(?:"([^"]*)"|([^;]+))/.exec(cabecalho);
  const nome = simples?.[1] ?? simples?.[2];
  return nome ? nome.trim() : null;
}

export const http = {
  get<T>(caminho: string, opcoes?: OpcoesRequisicao): Promise<T> {
    return requisitarJson<T>({ metodo: 'GET', caminho, ...opcoes });
  },

  post<T>(caminho: string, corpo?: unknown, opcoes?: OpcoesRequisicao): Promise<T> {
    return requisitarJson<T>({ metodo: 'POST', caminho, json: corpo, ...opcoes });
  },

  put<T>(caminho: string, corpo?: unknown, opcoes?: OpcoesRequisicao): Promise<T> {
    return requisitarJson<T>({ metodo: 'PUT', caminho, json: corpo, ...opcoes });
  },

  delete<T = void>(caminho: string, opcoes?: OpcoesRequisicao): Promise<T> {
    return requisitarJson<T>({ metodo: 'DELETE', caminho, ...opcoes });
  },

  /** Envio multipart/form-data (ex.: upload do XML de NF-e). */
  upload<T>(caminho: string, formData: FormData, opcoes?: OpcoesRequisicao): Promise<T> {
    return requisitarJson<T>({ metodo: 'POST', caminho, formData, ...opcoes });
  },

  /** Download binário (ex.: CSV da comparação). */
  async download(caminho: string, opcoes?: OpcoesRequisicao): Promise<ArquivoBaixado> {
    const resposta = await enviar({ metodo: 'GET', caminho, ...opcoes });
    return {
      blob: await resposta.blob(),
      nomeArquivo: nomeDoContentDisposition(resposta.headers.get('Content-Disposition')),
    };
  },
};
