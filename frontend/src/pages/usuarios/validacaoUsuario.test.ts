import { describe, expect, it } from 'vitest';
import { paraCriarUsuarioRequest, validarUsuario, type ValoresUsuario } from './validacaoUsuario';

/** Dados fictícios, exclusivos para testes. */
const validos: ValoresUsuario = {
  nome: 'Usuário Teste',
  email: 'usuario@teste.local',
  senha: 'senha-de-teste',
  confirmacaoSenha: 'senha-de-teste',
  perfil: 'USER',
};

function errosDe(valores: ValoresUsuario) {
  return Object.values(validarUsuario(valores)).filter(Boolean);
}

describe('validarUsuario', () => {
  it('aceita dados completos com perfil USER ou ADMIN', () => {
    expect(errosDe(validos)).toEqual([]);
    expect(errosDe({ ...validos, perfil: 'ADMIN' })).toEqual([]);
  });

  it('exige todos os campos, inclusive o perfil', () => {
    expect(
      validarUsuario({ nome: ' ', email: '', senha: '', confirmacaoSenha: '', perfil: '' }),
    ).toEqual({
      nome: 'O nome é obrigatório.',
      email: 'O e-mail é obrigatório.',
      senha: 'A senha é obrigatória.',
      confirmacaoSenha: 'A confirmação da senha é obrigatória.',
      perfil: 'O perfil é obrigatório (ADMIN ou USER).',
    });
  });

  it('valida o formato do e-mail, o tamanho da senha e a confirmação', () => {
    expect(validarUsuario({ ...validos, email: 'sem-arroba' }).email).toBe(
      'O e-mail informado é inválido.',
    );
    expect(validarUsuario({ ...validos, senha: 'curta', confirmacaoSenha: 'curta' }).senha).toBe(
      'A senha deve ter entre 8 e 72 caracteres.',
    );
    expect(validarUsuario({ ...validos, senha: 'a'.repeat(73) }).senha).toBe(
      'A senha deve ter entre 8 e 72 caracteres.',
    );
    expect(validarUsuario({ ...validos, confirmacaoSenha: 'outra-senha' }).confirmacaoSenha).toBe(
      'A confirmação da senha não confere.',
    );
  });
});

describe('paraCriarUsuarioRequest', () => {
  it('remove espaços do nome e do e-mail, mantém a senha e não envia a confirmação', () => {
    expect(
      paraCriarUsuarioRequest({
        ...validos,
        nome: '  Usuário Teste ',
        email: ' usuario@teste.local ',
        senha: ' senha com espaços ',
        perfil: 'ADMIN',
      }),
    ).toEqual({
      nome: 'Usuário Teste',
      email: 'usuario@teste.local',
      senha: ' senha com espaços ',
      perfil: 'ADMIN',
    });
  });
});
