import type { ErrosFormulario } from '../../hooks/useFormulario';
import type { Perfil } from '../../types/auth';
import type { CriarUsuarioRequest } from '../../types/usuario';
import {
  validarConfiguracaoInicial,
  type ValoresConfiguracaoInicial,
} from '../configuracao-inicial/validacaoConfiguracaoInicial';

/** Valores do formulário de usuário, como digitados; perfil vazio enquanto não escolhido. */
export interface ValoresUsuario extends ValoresConfiguracaoInicial {
  perfil: Perfil | '';
}

export const VALORES_INICIAIS_USUARIO: ValoresUsuario = {
  nome: '',
  email: '',
  senha: '',
  confirmacaoSenha: '',
  perfil: '',
};

export const CAMPOS_USUARIO: string[] = Object.keys(VALORES_INICIAIS_USUARIO);

export function ehPerfil(valor: string | null): valor is Perfil {
  return valor === 'ADMIN' || valor === 'USER';
}

/**
 * Nome, e-mail, senha e confirmação seguem as mesmas regras do primeiro ADMIN (mesmos limites
 * de CriarUsuarioRequest); o perfil é escolhido explicitamente, sem valor padrão.
 */
export function validarUsuario(valores: ValoresUsuario): ErrosFormulario<ValoresUsuario> {
  return {
    ...validarConfiguracaoInicial(valores),
    perfil: ehPerfil(valores.perfil) ? undefined : 'O perfil é obrigatório (ADMIN ou USER).',
  };
}

/**
 * Converte os valores já validados. A confirmação só é conferida na interface: o backend não a
 * recebe. A senha é enviada exatamente como digitada.
 */
export function paraCriarUsuarioRequest(valores: ValoresUsuario): CriarUsuarioRequest {
  if (!ehPerfil(valores.perfil)) {
    throw new Error('Perfil não validado.');
  }
  return {
    nome: valores.nome.trim(),
    email: valores.email.trim(),
    senha: valores.senha,
    perfil: valores.perfil,
  };
}
