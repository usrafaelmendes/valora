import { describe, expect, it } from 'vitest';
import { paraProdutoRequest, validarProduto } from './validacaoProduto';

const validos = { nome: 'Produto Teste A', descricao: '', gtin: '' };

describe('validarProduto', () => {
  it('aceita produto com nome e campos opcionais vazios', () => {
    expect(Object.values(validarProduto(validos)).filter(Boolean)).toEqual([]);
  });

  it('exige o nome e respeita os limites de tamanho do backend', () => {
    expect(validarProduto({ ...validos, nome: '   ' }).nome).toBe('O nome é obrigatório.');
    expect(validarProduto({ ...validos, nome: 'a'.repeat(151) }).nome).toBe(
      'O nome deve ter no máximo 150 caracteres.',
    );
    expect(validarProduto({ ...validos, descricao: 'a'.repeat(501) }).descricao).toBe(
      'A descrição deve ter no máximo 500 caracteres.',
    );
  });

  it('aceita GTIN com 8, 12, 13 ou 14 dígitos e rejeita outros formatos', () => {
    for (const gtin of ['12345670', '123456789012', '7891234567895', '17891234567892']) {
      expect(validarProduto({ ...validos, gtin }).gtin).toBeUndefined();
    }
    for (const gtin of ['1234567', '12345678901', 'ABC45678', '789-1234567895']) {
      expect(validarProduto({ ...validos, gtin }).gtin).toMatch(/GTIN\/EAN informado é inválido/);
    }
  });
});

describe('paraProdutoRequest', () => {
  it('remove espaços e envia opcionais em branco como null', () => {
    expect(paraProdutoRequest({ nome: ' Produto Teste B ', descricao: '  ', gtin: '' })).toEqual({
      nome: 'Produto Teste B',
      descricao: null,
      gtin: null,
    });
  });
});
