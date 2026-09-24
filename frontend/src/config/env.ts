/**
 * Configuração do frontend lida em tempo de build (variáveis VITE_*).
 * Nenhum segredo deve ser colocado aqui: tudo o que o Vite injeta vai para o navegador.
 */

/**
 * Prefixo de todas as chamadas à API.
 * - web: `/api`; o proxy do Vite repassa `/api/...` para o backend sem o prefixo;
 * - desktop (Tauri, modo `desktop` do Vite): a URL do backend local, definida em vite.config.ts,
 *   pois no app empacotado não há proxy.
 */
export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api';
