import type { Fornecedor, FornecedorRequest } from '../types/fornecedor';
import { http } from './httpClient';

/** Chave do TanStack Query da listagem; invalidada após cadastro, edição e desativação. */
export const CHAVE_FORNECEDORES = ['fornecedores'] as const;

/** Endpoints de fornecedores (FornecedorController). Escrita restrita ao ADMIN pelo backend. */
export const fornecedoresApi = {
  /** Somente fornecedores ativos, ordenados pela razão social. */
  listar(signal?: AbortSignal): Promise<Fornecedor[]> {
    return http.get<Fornecedor[]>('/fornecedores', { signal });
  },

  criar(dados: FornecedorRequest): Promise<Fornecedor> {
    return http.post<Fornecedor>('/fornecedores', dados);
  },

  atualizar(id: number, dados: FornecedorRequest): Promise<Fornecedor> {
    return http.put<Fornecedor>(`/fornecedores/${String(id)}`, dados);
  },

  /** Desativação lógica: o fornecedor é mantido no banco para o histórico. */
  desativar(id: number): Promise<void> {
    return http.delete(`/fornecedores/${String(id)}`);
  },
};
