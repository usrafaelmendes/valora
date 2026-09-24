import type { Perfil } from './auth';

/**
 * Contrato de criação de usuário conforme UsuarioController e CriarUsuarioRequest.
 * A resposta é o mesmo UsuarioResponse de /auth/me (tipo Usuario em ./auth).
 */
export interface CriarUsuarioRequest {
  nome: string;
  email: string;
  senha: string;
  perfil: Perfil;
}
