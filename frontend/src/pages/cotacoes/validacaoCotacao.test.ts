import { describe, expect, it } from 'vitest';
import {
  errosDaOpcao,
  paraCotacaoRequest,
  paraOpcaoRequest,
  semErros,
  validarCotacao,
  validarOpcao,
  valoresIniciaisOpcao,
  type ValoresOpcao,
} from './validacaoCotacao';

function opcao(extra: Record<string, string>): ValoresOpcao {
  return { ...valoresIniciaisOpcao(), ...extra };
}

describe('validarCotacao', () => {
  it('exige produto e quantidade maior que zero, com as mensagens do backend', () => {
    expect(validarCotacao({ produtoId: '', quantidade: '', descricao: '' })).toMatchObject({
      produtoId: 'O produto é obrigatório.',
      quantidade: 'A quantidade é obrigatória.',
    });
    expect(validarCotacao({ produtoId: '1', quantidade: '0', descricao: '' }).quantidade).toBe(
      'A quantidade deve ser maior que zero.',
    );
    expect(validarCotacao({ produtoId: '1', quantidade: '-2', descricao: '' }).quantidade).toBe(
      'A quantidade deve ser maior que zero.',
    );
    expect(
      validarCotacao({ produtoId: '1', quantidade: '1,12345', descricao: '' }).quantidade,
    ).toBe('A quantidade aceita até 11 dígitos inteiros e 4 decimais.');
  });

  it('aceita quantidade com vírgula e limita a descrição a 500 caracteres', () => {
    expect(semErros(validarCotacao({ produtoId: '1', quantidade: '2,5', descricao: '' }))).toBe(
      true,
    );
    expect(
      validarCotacao({ produtoId: '1', quantidade: '2', descricao: 'x'.repeat(501) }).descricao,
    ).toBe('A descrição deve ter no máximo 500 caracteres.');
  });
});

describe('validarOpcao', () => {
  it('exige o fornecedor ou o item de NF-e', () => {
    expect(validarOpcao(opcao({})).fornecedorId).toBe(
      'Informe o fornecedor ou o item de NF-e da opção.',
    );
    expect(validarOpcao(opcao({ nfeItemId: '101' })).fornecedorId).toBeUndefined();
    expect(semErros(validarOpcao(opcao({ fornecedorId: '10' })))).toBe(true);
  });

  it('valida valores, alíquotas e CFOP como o backend', () => {
    const erros = validarOpcao(
      opcao({
        fornecedorId: '10',
        'valores.valorProduto': '-1',
        'valores.valorFrete': '10,123',
        'dadosFiscais.aliquotaIcms': '101',
        'dadosFiscais.aliquotaIpi': '7,12345',
        'dadosFiscais.cfop': '61',
      }),
    );
    expect(erros).toMatchObject({
      'valores.valorProduto': 'O valor não pode ser negativo.',
      'valores.valorFrete': 'Use no máximo 13 dígitos inteiros e 2 decimais.',
      'dadosFiscais.aliquotaIcms': 'A alíquota deve estar entre 0 e 100.',
      'dadosFiscais.aliquotaIpi': 'A alíquota aceita no máximo 4 casas decimais.',
      'dadosFiscais.cfop': 'O CFOP deve ter 4 dígitos.',
    });
  });
});

describe('paraOpcaoRequest', () => {
  it('envia grupos não preenchidos como null (não informado)', () => {
    expect(paraOpcaoRequest(opcao({ fornecedorId: '10', condicaoPagamento: '  ' }))).toEqual({
      fornecedorId: 10,
      nfeItemId: null,
      condicaoPagamento: null,
      observacao: null,
      valores: null,
      dadosFiscais: null,
    });
  });

  it('mantém componente em branco como null e zero digitado como zero', () => {
    const request = paraOpcaoRequest(
      opcao({
        fornecedorId: '10',
        'valores.valorProduto': '1234,5',
        'valores.valorFrete': '0',
        'dadosFiscais.aliquotaPis': '1,35',
        'dadosFiscais.origemMercadoria': '6',
      }),
    );
    expect(request.valores).toEqual({
      valorProduto: 1234.5,
      valorIpi: null,
      valorFrete: 0,
      valorSeguro: null,
      valorOutrasDespesas: null,
      valorDesconto: null,
    });
    expect(request.dadosFiscais).toEqual({
      origemMercadoria: '6',
      cfop: null,
      aliquotaIcms: null,
      aliquotaIpi: null,
      aliquotaPis: 1.35,
      aliquotaCofins: null,
    });
  });
});

describe('paraCotacaoRequest', () => {
  it('monta a cotação com todas as opções na ordem do formulário', () => {
    const request = paraCotacaoRequest({ produtoId: '20', quantidade: '2,5', descricao: '' }, [
      opcao({ fornecedorId: '10' }),
      opcao({ fornecedorId: '11' }),
    ]);
    expect(request).toMatchObject({ produtoId: 20, quantidade: 2.5, descricao: null });
    expect(request.opcoes.map((item) => item.fornecedorId)).toEqual([10, 11]);
  });
});

describe('errosDaOpcao', () => {
  it('associa os erros da API à opção pelo índice e ignora campos de fora do formulário', () => {
    const erros = {
      'opcoes[1].valores.valorProduto': 'O valor não pode ser negativo.',
      'opcoes[1].fornecedorId': 'Fornecedor não encontrado ou desativado.',
      'opcoes[0].fornecedorId': 'Outro erro.',
      'opcoes[1].desconhecido': 'Ignorado.',
      produtoId: 'Produto não encontrado ou desativado.',
    };
    expect(errosDaOpcao(erros, 1)).toEqual({
      'valores.valorProduto': 'O valor não pode ser negativo.',
      fornecedorId: 'Fornecedor não encontrado ou desativado.',
    });
  });
});
