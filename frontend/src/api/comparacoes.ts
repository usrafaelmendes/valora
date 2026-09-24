import type { Comparacao } from '../types/cotacao';
import { http, type ArquivoBaixado } from './httpClient';

export function chaveComparacao(id: number) {
  return ['comparacoes', id] as const;
}

/** Endpoints de comparações já executadas (ComparacaoController). Qualquer usuário autenticado. */
export const comparacoesApi = {
  buscar(id: number, signal?: AbortSignal): Promise<Comparacao> {
    return http.get<Comparacao>(`/comparacoes/${String(id)}`, { signal });
  },

  /**
   * Tabela da comparação gravada (CSV montado pelo backend). O frontend não gera, reordena
   * nem recalcula o conteúdo: apenas baixa o arquivo devolvido.
   */
  download(id: number): Promise<ArquivoBaixado> {
    return http.download(`/comparacoes/${String(id)}/download`);
  },
};
