/** Caminhos das páginas do frontend (não confundir com os endpoints da API). */
export const ROTAS = {
  login: '/login',
  configuracaoInicial: '/configuracao-inicial',
  inicio: '/',
  cotacoes: '/cotacoes',
  novaCotacao: '/cotacoes/nova',
  cotacaoDetalhe: '/cotacoes/:id',
  comparacao: '/comparacoes/:id',
  produtos: '/produtos',
  fornecedores: '/fornecedores',
  nfe: '/nfe',
  nfeDetalhe: '/nfe/:id',
  regrasTributarias: '/regras-tributarias',
  regrasAplicaveis: '/regras-tributarias/aplicaveis',
  parametrosCalculo: '/parametros-calculo',
  usuarios: '/usuarios',
} as const;

export function caminhoCotacao(id: number): string {
  return `/cotacoes/${String(id)}`;
}

export function caminhoComparacao(id: number): string {
  return `/comparacoes/${String(id)}`;
}

/** Aviso exibido na página de destino após uma operação concluída em outra página. */
export interface EstadoAviso {
  aviso?: string;
}

export function caminhoDetalheNfe(id: number): string {
  return `/nfe/${String(id)}`;
}

/** Estado enviado ao redirecionar para o login, para voltar à página pedida depois. */
export interface EstadoRedirecionamentoLogin {
  de?: string;
}
