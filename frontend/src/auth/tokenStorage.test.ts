import { describe, expect, it } from 'vitest';
import { expiraEmFuturo, TOKEN_TESTE } from '../test/fetchFalso';
import { tokenStorage } from './tokenStorage';

const CHAVE = 'compara-precos.sessao';

describe('tokenStorage', () => {
  it('salva e recupera o token em sessionStorage', () => {
    const dados = { token: TOKEN_TESTE, expiraEm: expiraEmFuturo() };
    tokenStorage.salvar(dados);

    expect(sessionStorage.getItem(CHAVE)).not.toBeNull();
    expect(tokenStorage.ler()).toEqual(dados);
  });

  it('retorna null sem sessão salva', () => {
    expect(tokenStorage.ler()).toBeNull();
  });

  it('descarta token expirado', () => {
    tokenStorage.salvar({
      token: TOKEN_TESTE,
      expiraEm: new Date(Date.now() - 1000).toISOString(),
    });

    expect(tokenStorage.ler()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });

  it('descarta conteúdo corrompido', () => {
    sessionStorage.setItem(CHAVE, '{não é json');

    expect(tokenStorage.ler()).toBeNull();
    expect(sessionStorage.getItem(CHAVE)).toBeNull();
  });

  it('limpa a sessão', () => {
    tokenStorage.salvar({ token: TOKEN_TESTE, expiraEm: expiraEmFuturo() });
    tokenStorage.limpar();

    expect(tokenStorage.ler()).toBeNull();
  });
});
