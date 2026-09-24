/**
 * Contratos de autenticação conforme AuthController, LoginRequest/LoginResponse, UsuarioResponse
 * e os DTOs da configuração inicial.
 */

export type Perfil = 'ADMIN' | 'USER';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  /** Instante de expiração do token (ISO-8601). */
  expiraEm: string;
  usuario: Usuario;
}

export interface StatusConfiguracaoInicial {
  /** true assim que existe qualquer usuário; a configuração inicial deixa de estar disponível. */
  configurado: boolean;
}

/** Dados do primeiro ADMIN; o perfil é definido pelo backend. */
export interface ConfiguracaoInicialRequest {
  nome: string;
  email: string;
  senha: string;
  confirmacaoSenha: string;
}
