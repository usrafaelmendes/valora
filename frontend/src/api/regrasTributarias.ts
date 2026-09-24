import type {
  FiltroRegras,
  RegraTributaria,
  RegraTributariaRequest,
  RegrasAplicaveisRequest,
  RegrasAplicaveisResponse,
} from '../types/regraTributaria';
import { http } from './httpClient';

/** Prefixo das chaves do TanStack Query; invalidado após cadastro, edição e desativação. */
export const CHAVE_REGRAS_TRIBUTARIAS = ['regras-tributarias'] as const;

/** Endpoints de regras tributárias (RegraTributariaController). Escrita restrita ao ADMIN. */
export const regrasTributariasApi = {
  /** Ordenadas pelo backend por tributo, prioridade (maior primeiro) e nome. */
  listar(filtro: FiltroRegras = {}, signal?: AbortSignal): Promise<RegraTributaria[]> {
    const parametros = new URLSearchParams();
    if (filtro.ativa !== undefined) {
      parametros.set('ativa', String(filtro.ativa));
    }
    if (filtro.tributo) {
      parametros.set('tributo', filtro.tributo);
    }
    const consulta = parametros.toString();
    return http.get<RegraTributaria[]>(`/regras-tributarias${consulta ? `?${consulta}` : ''}`, {
      signal,
    });
  },

  criar(dados: RegraTributariaRequest): Promise<RegraTributaria> {
    return http.post<RegraTributaria>('/regras-tributarias', dados);
  },

  /** Gera uma nova versão da regra; cálculos já gravados guardam a versão que usaram. */
  atualizar(id: number, dados: RegraTributariaRequest): Promise<RegraTributaria> {
    return http.put<RegraTributaria>(`/regras-tributarias/${String(id)}`, dados);
  },

  /** Desativação lógica: a regra continua cadastrada e pode ser reativada pela edição. */
  desativar(id: number): Promise<void> {
    return http.delete(`/regras-tributarias/${String(id)}`);
  },

  /** Quais regras ativas o backend selecionaria para uma operação de exemplo (sem calcular). */
  aplicaveis(dados: RegrasAplicaveisRequest): Promise<RegrasAplicaveisResponse> {
    return http.post<RegrasAplicaveisResponse>('/regras-tributarias/aplicaveis', dados);
  },
};
