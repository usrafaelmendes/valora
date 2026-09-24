import { excedeLimite, type ErrosFormulario } from '../../hooks/useFormulario';
import type { ConfiguracaoInicialRequest } from '../../types/auth';

/** Valores do formulário do primeiro ADMIN, como digitados. */
export interface ValoresConfiguracaoInicial extends Record<string, string> {
  nome: string;
  email: string;
  senha: string;
  confirmacaoSenha: string;
}

export const VALORES_INICIAIS_CONFIGURACAO: ValoresConfiguracaoInicial = {
  nome: '',
  email: '',
  senha: '',
  confirmacaoSenha: '',
};

/** Limites de UsuarioService.SENHA_MIN/SENHA_MAX no backend. */
export const SENHA_MIN = 8;
export const SENHA_MAX = 72;

/** Verificação simples; o formato definitivo do e-mail é validado pela API. */
const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+$/;

/** Espelha as restrições de ConfiguracaoInicialRequest, com as mesmas mensagens do backend. */
export function validarConfiguracaoInicial(
  valores: ValoresConfiguracaoInicial,
): ErrosFormulario<ValoresConfiguracaoInicial> {
  const email = valores.email.trim();
  return {
    nome: !valores.nome.trim()
      ? 'O nome é obrigatório.'
      : excedeLimite(valores.nome, 150, 'O nome deve ter no máximo 150 caracteres.'),
    email: !email
      ? 'O e-mail é obrigatório.'
      : !FORMATO_EMAIL.test(email)
        ? 'O e-mail informado é inválido.'
        : excedeLimite(email, 255, 'O e-mail deve ter no máximo 255 caracteres.'),
    senha: !valores.senha.trim()
      ? 'A senha é obrigatória.'
      : valores.senha.length < SENHA_MIN || valores.senha.length > SENHA_MAX
        ? `A senha deve ter entre ${String(SENHA_MIN)} e ${String(SENHA_MAX)} caracteres.`
        : undefined,
    confirmacaoSenha: !valores.confirmacaoSenha
      ? 'A confirmação da senha é obrigatória.'
      : valores.confirmacaoSenha !== valores.senha
        ? 'A confirmação da senha não confere.'
        : undefined,
  };
}

/** Converte os valores já validados; a senha é enviada exatamente como digitada. */
export function paraConfiguracaoInicialRequest(
  valores: ValoresConfiguracaoInicial,
): ConfiguracaoInicialRequest {
  return {
    nome: valores.nome.trim(),
    email: valores.email.trim(),
    senha: valores.senha,
    confirmacaoSenha: valores.confirmacaoSenha,
  };
}
