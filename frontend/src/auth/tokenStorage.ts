/**
 * Persistência do token JWT em sessionStorage: sobrevive ao reload da aba,
 * mas é descartada ao fechar a aba/navegador. Não há refresh token no backend.
 */

const CHAVE = 'compara-precos.sessao';

export interface TokenArmazenado {
  token: string;
  /** Instante de expiração informado pelo backend (ISO-8601). */
  expiraEm: string;
}

function expirado(expiraEm: string, agora: number): boolean {
  const instante = Date.parse(expiraEm);
  return Number.isNaN(instante) || instante <= agora;
}

export const tokenStorage = {
  salvar(dados: TokenArmazenado): void {
    sessionStorage.setItem(CHAVE, JSON.stringify(dados));
  },

  /** Token válido armazenado; ausente, corrompido ou expirado resulta em null (e é removido). */
  ler(agora: number = Date.now()): TokenArmazenado | null {
    const bruto = sessionStorage.getItem(CHAVE);
    if (!bruto) {
      return null;
    }
    try {
      const dados = JSON.parse(bruto) as Partial<TokenArmazenado> | null;
      if (
        typeof dados?.token === 'string' &&
        typeof dados.expiraEm === 'string' &&
        !expirado(dados.expiraEm, agora)
      ) {
        return { token: dados.token, expiraEm: dados.expiraEm };
      }
    } catch {
      // Conteúdo corrompido: tratado como ausência de sessão.
    }
    sessionStorage.removeItem(CHAVE);
    return null;
  },

  limpar(): void {
    sessionStorage.removeItem(CHAVE);
  },
};
