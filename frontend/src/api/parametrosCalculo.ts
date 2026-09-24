import type { ParametroCalculo, ParametroCalculoRequest } from '../types/parametroCalculo';
import { http } from './httpClient';

/** Chave do TanStack Query da listagem; invalidada após cada alteração. */
export const CHAVE_PARAMETROS_CALCULO = ['parametros-calculo'] as const;

/** Endpoints de parâmetros de cálculo (ParametroCalculoController). Alteração restrita ao ADMIN. */
export const parametrosCalculoApi = {
  /** Na ordem definida pelo backend. */
  listar(signal?: AbortSignal): Promise<ParametroCalculo[]> {
    return http.get<ParametroCalculo[]>('/parametros-calculo', { signal });
  },

  /** O backend valida e normaliza o valor; nulo volta a "não definido". */
  atualizar(chave: string, dados: ParametroCalculoRequest): Promise<ParametroCalculo> {
    return http.put<ParametroCalculo>(`/parametros-calculo/${encodeURIComponent(chave)}`, dados);
  },
};
