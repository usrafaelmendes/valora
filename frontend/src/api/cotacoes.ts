import type {
  Comparacao,
  ComparacaoResumo,
  Cotacao,
  CotacaoRequest,
  CotacaoResumo,
  OpcaoCotacao,
  OpcaoCotacaoRequest,
} from '../types/cotacao';
import { http } from './httpClient';

/** Chave das listagens de cotações; invalidada após criar uma cotação. */
export const CHAVE_COTACOES = ['cotacoes'] as const;

export function chaveListaCotacoes(produtoId: number | null) {
  return ['cotacoes', 'lista', produtoId] as const;
}

export function chaveCotacao(id: number) {
  return ['cotacoes', id] as const;
}

export function chaveComparacoesDaCotacao(id: number) {
  return ['cotacoes', id, 'comparacoes'] as const;
}

/**
 * Endpoints de cotações (CotacaoController). Qualquer usuário autenticado (ADMIN ou USER).
 * Cotações, opções e comparações são históricas: não há alteração nem exclusão pela API.
 */
export const cotacoesApi = {
  /** Da mais recente para a mais antiga; filtro opcional por produto. */
  listar(produtoId: number | null, signal?: AbortSignal): Promise<CotacaoResumo[]> {
    const filtro = produtoId === null ? '' : `?produtoId=${String(produtoId)}`;
    return http.get<CotacaoResumo[]>(`/cotacoes${filtro}`, { signal });
  },

  /** Cotação com as opções e a comparação mais recente. */
  buscar(id: number, signal?: AbortSignal): Promise<Cotacao> {
    return http.get<Cotacao>(`/cotacoes/${String(id)}`, { signal });
  },

  /** Cria a cotação; o backend já executa a primeira comparação. */
  criar(dados: CotacaoRequest): Promise<Cotacao> {
    return http.post<Cotacao>('/cotacoes', dados);
  },

  /** Inclui uma opção; ela entra somente na próxima comparação executada. */
  adicionarOpcao(cotacaoId: number, dados: OpcaoCotacaoRequest): Promise<OpcaoCotacao> {
    return http.post<OpcaoCotacao>(`/cotacoes/${String(cotacaoId)}/opcoes`, dados);
  },

  /** Nova comparação com a configuração vigente; as anteriores continuam no histórico. */
  comparar(cotacaoId: number): Promise<Comparacao> {
    return http.post<Comparacao>(`/cotacoes/${String(cotacaoId)}/comparacoes`);
  },

  /** Histórico da cotação, da comparação mais recente para a mais antiga. */
  listarComparacoes(cotacaoId: number, signal?: AbortSignal): Promise<ComparacaoResumo[]> {
    return http.get<ComparacaoResumo[]>(`/cotacoes/${String(cotacaoId)}/comparacoes`, { signal });
  },
};
