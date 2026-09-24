import type { Produto, ProdutoRequest } from '../types/produto';
import { http } from './httpClient';

/** Chave do TanStack Query da listagem; invalidada após cadastro, edição e desativação. */
export const CHAVE_PRODUTOS = ['produtos'] as const;

/** Endpoints de produtos (ProdutoController). Escrita restrita ao ADMIN pelo backend. */
export const produtosApi = {
  /** Somente produtos ativos, ordenados pelo nome. */
  listar(signal?: AbortSignal): Promise<Produto[]> {
    return http.get<Produto[]>('/produtos', { signal });
  },

  criar(dados: ProdutoRequest): Promise<Produto> {
    return http.post<Produto>('/produtos', dados);
  },

  atualizar(id: number, dados: ProdutoRequest): Promise<Produto> {
    return http.put<Produto>(`/produtos/${String(id)}`, dados);
  },

  /** Desativação lógica: o produto é mantido no banco para o histórico. */
  desativar(id: number): Promise<void> {
    return http.delete(`/produtos/${String(id)}`);
  },
};
