import { describe, expect, it } from 'vitest';
import { paraFornecedorRequest, validarFornecedor } from './validacaoFornecedor';
import type { ValoresFornecedor } from './validacaoFornecedor';

const validos: ValoresFornecedor = {
  razaoSocial: 'Fornecedor Teste Ltda',
  cnpj: '11.222.333/0001-81',
  uf: 'GO',
  tipo: 'FABRICANTE',
  prazoPagamentoBase: '',
};

describe('validarFornecedor', () => {
  it('aceita fornecedor válido com prazo não informado', () => {
    expect(Object.values(validarFornecedor(validos)).filter(Boolean)).toEqual([]);
  });

  it('informa todos os campos obrigatórios ausentes', () => {
    const erros = validarFornecedor({
      razaoSocial: '',
      cnpj: '',
      uf: '',
      tipo: '',
      prazoPagamentoBase: '',
    });
    expect(erros).toMatchObject({
      razaoSocial: 'A razão social é obrigatória.',
      cnpj: 'O CNPJ é obrigatório.',
      uf: 'A UF de emissão é obrigatória.',
      tipo: 'O tipo do fornecedor é obrigatório.',
    });
  });

  it('aceita CNPJ com ou sem máscara, numérico ou alfanumérico, e rejeita outros formatos', () => {
    for (const cnpj of ['11222333000181', '11.222.333/0001-81', '12.abc.345/01de-35']) {
      expect(validarFornecedor({ ...validos, cnpj }).cnpj).toBeUndefined();
    }
    for (const cnpj of ['1122233300018', '11.222.333/0001-8A', 'CNPJ']) {
      expect(validarFornecedor({ ...validos, cnpj }).cnpj).toBe('O CNPJ informado é inválido.');
    }
  });

  it('respeita os limites de tamanho do backend', () => {
    expect(validarFornecedor({ ...validos, razaoSocial: 'a'.repeat(151) }).razaoSocial).toBe(
      'A razão social deve ter no máximo 150 caracteres.',
    );
    expect(
      validarFornecedor({ ...validos, prazoPagamentoBase: 'a'.repeat(101) }).prazoPagamentoBase,
    ).toBe('O prazo de pagamento base deve ter no máximo 100 caracteres.');
  });
});

describe('paraFornecedorRequest', () => {
  it('envia o CNPJ sem máscara e o prazo em branco como null', () => {
    expect(paraFornecedorRequest({ ...validos, razaoSocial: ' Fornecedor Teste Ltda ' })).toEqual({
      razaoSocial: 'Fornecedor Teste Ltda',
      cnpj: '11222333000181',
      uf: 'GO',
      tipo: 'FABRICANTE',
      prazoPagamentoBase: null,
    });
  });
});
