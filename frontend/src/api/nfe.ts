import type { Nfe, NfeResumo } from '../types/nfe';
import { http } from './httpClient';

/** Chave do TanStack Query da listagem; invalidada após cada importação. */
export const CHAVE_NFES = ['nfe'] as const;

export function chaveNfe(id: number) {
  return ['nfe', id] as const;
}

/** Endpoints de NF-e (NfeController). Todos restritos ao ADMIN pelo backend. */
export const nfeApi = {
  /** Mais recentes primeiro (data de emissão). */
  listar(signal?: AbortSignal): Promise<NfeResumo[]> {
    return http.get<NfeResumo[]>('/nfe', { signal });
  },

  buscar(id: number, signal?: AbortSignal): Promise<Nfe> {
    return http.get<Nfe>(`/nfe/${String(id)}`, { signal });
  },

  /** Upload manual do XML (multipart, campo "arquivo"); o backend valida e processa. */
  importar(arquivo: File): Promise<Nfe> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    return http.upload<Nfe>('/nfe', formData);
  },
};
