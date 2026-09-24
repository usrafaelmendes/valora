import { describe, expect, it } from 'vitest';
import {
  paraConfiguracaoInicialRequest,
  validarConfiguracaoInicial,
  VALORES_INICIAIS_CONFIGURACAO,
} from './validacaoConfiguracaoInicial';

const VALIDOS = {
  nome: 'Admin Teste',
  email: 'admin@teste.local',
  senha: 'senha-de-teste',
  confirmacaoSenha: 'senha-de-teste',
};

function semErros(erros: object) {
  return Object.values(erros).every((mensagem) => !mensagem);
}

describe('validarConfiguracaoInicial', () => {
  it('aceita dados válidos', () => {
    expect(semErros(validarConfiguracaoInicial(VALIDOS))).toBe(true);
  });

  it('exige todos os campos', () => {
    expect(validarConfiguracaoInicial(VALORES_INICIAIS_CONFIGURACAO)).toEqual({
      nome: 'O nome é obrigatório.',
      email: 'O e-mail é obrigatório.',
      senha: 'A senha é obrigatória.',
      confirmacaoSenha: 'A confirmação da senha é obrigatória.',
    });
  });

  it('rejeita nome só com espaços e nome longo', () => {
    expect(validarConfiguracaoInicial({ ...VALIDOS, nome: '   ' }).nome).toBe(
      'O nome é obrigatório.',
    );
    expect(validarConfiguracaoInicial({ ...VALIDOS, nome: 'a'.repeat(151) }).nome).toBe(
      'O nome deve ter no máximo 150 caracteres.',
    );
  });

  it('rejeita e-mail em formato inválido', () => {
    expect(validarConfiguracaoInicial({ ...VALIDOS, email: 'nao-e-email' }).email).toBe(
      'O e-mail informado é inválido.',
    );
  });

  it('exige senha entre 8 e 72 caracteres', () => {
    const curta = 'a'.repeat(7);
    const longa = 'a'.repeat(73);
    const mensagem = 'A senha deve ter entre 8 e 72 caracteres.';
    expect(
      validarConfiguracaoInicial({ ...VALIDOS, senha: curta, confirmacaoSenha: curta }).senha,
    ).toBe(mensagem);
    expect(
      validarConfiguracaoInicial({ ...VALIDOS, senha: longa, confirmacaoSenha: longa }).senha,
    ).toBe(mensagem);
  });

  it('rejeita confirmação diferente da senha', () => {
    expect(
      validarConfiguracaoInicial({ ...VALIDOS, confirmacaoSenha: 'outra-senha' }).confirmacaoSenha,
    ).toBe('A confirmação da senha não confere.');
  });

  it('envia nome e e-mail sem espaços nas pontas e a senha como digitada', () => {
    expect(
      paraConfiguracaoInicialRequest({
        ...VALIDOS,
        nome: '  Admin Teste ',
        email: ' admin@teste.local ',
        senha: ' senha com espaço ',
        confirmacaoSenha: ' senha com espaço ',
      }),
    ).toEqual({
      nome: 'Admin Teste',
      email: 'admin@teste.local',
      senha: ' senha com espaço ',
      confirmacaoSenha: ' senha com espaço ',
    });
  });
});
