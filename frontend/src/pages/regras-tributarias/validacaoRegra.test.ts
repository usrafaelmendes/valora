import { describe, expect, it } from 'vitest';
import type { RegraTributaria } from '../../types/regraTributaria';
import {
  paraRegraRequest,
  validarRegra,
  valoresIniciaisRegra,
  type ValoresRegra,
} from './validacaoRegra';

/** Valores fictícios de configuração, só para testar o formulário. */
function valores(parcial: Partial<ValoresRegra>): ValoresRegra {
  return {
    ...valoresIniciaisRegra(null),
    nome: 'Regra de teste',
    tributo: 'ICMS',
    formaAliquota: 'ALIQUOTA_DA_NFE',
    ...parcial,
  };
}

function erros(parcial: Partial<ValoresRegra>) {
  return Object.fromEntries(
    Object.entries(validarRegra(valores(parcial))).filter(([, mensagem]) => mensagem),
  );
}

describe('validarRegra', () => {
  it('exige nome, tributo e forma de obtenção da alíquota', () => {
    expect(erros({ nome: ' ', tributo: '', formaAliquota: '' })).toEqual({
      nome: 'O nome é obrigatório.',
      tributo: 'O tributo é obrigatório.',
      formaAliquota: 'A forma de obtenção da alíquota é obrigatória.',
    });
  });

  it('exige alíquota de 0 a 100 com até 4 casas somente para percentual fixo', () => {
    expect(erros({ formaAliquota: 'PERCENTUAL_FIXO' })).toEqual({
      aliquota: 'A alíquota é obrigatória para PERCENTUAL_FIXO.',
    });
    expect(erros({ formaAliquota: 'PERCENTUAL_FIXO', aliquota: '100,5' })).toEqual({
      aliquota: 'A alíquota deve estar entre 0 e 100.',
    });
    expect(erros({ formaAliquota: 'PERCENTUAL_FIXO', aliquota: '1,23456' })).toEqual({
      aliquota: 'A alíquota aceita no máximo 4 casas decimais.',
    });
    expect(erros({ formaAliquota: 'PERCENTUAL_FIXO', aliquota: '6,35' })).toEqual({});
    expect(erros({ formaAliquota: 'ALIQUOTA_DA_NFE', aliquota: 'abc' })).toEqual({});
  });

  it('valida fator e prioridade como no backend', () => {
    expect(erros({ fator: '0' })).toEqual({ fator: 'O fator deve ser maior que zero.' });
    expect(erros({ fator: '10' })).toEqual({
      fator: 'O fator deve ser menor que 10 e ter no máximo 4 casas decimais.',
    });
    expect(erros({ fator: '0,25' })).toEqual({});
    expect(erros({ formaAliquota: 'SEM_CREDITO', fator: '0' })).toEqual({});
    expect(erros({ prioridade: '1001' })).toEqual({
      prioridade: 'A prioridade deve estar entre 0 e 1000.',
    });
    expect(erros({ prioridade: '-1' })).toEqual({
      prioridade: 'A prioridade deve estar entre 0 e 1000.',
    });
  });

  it('exige CFOPs de 4 dígitos e limita os textos', () => {
    expect(erros({ cfops: '6102,61' })).toEqual({ cfops: 'Cada CFOP deve ter 4 dígitos.' });
    expect(erros({ nome: 'x'.repeat(151), observacao: 'y'.repeat(1001) })).toEqual({
      nome: 'O nome deve ter no máximo 150 caracteres.',
      observacao: 'A observação deve ter no máximo 1000 caracteres.',
    });
  });
});

describe('paraRegraRequest', () => {
  it('converte decimais com vírgula e deixa vazios os campos que não se aplicam', () => {
    expect(
      paraRegraRequest(
        valores({
          formaAliquota: 'SEM_CREDITO',
          aliquota: '4',
          fator: '0,25',
          prioridade: '',
          observacao: '  ',
          fornecedorId: '10',
          origensMercadoria: '1,6',
          cfops: '6102',
          ativa: 'false',
        }),
      ),
    ).toEqual({
      nome: 'Regra de teste',
      observacao: null,
      tributo: 'ICMS',
      formaAliquota: 'SEM_CREDITO',
      aliquota: null,
      fator: null,
      prioridade: null,
      ativa: false,
      tipoFornecedor: null,
      fornecedorId: 10,
      produtoId: null,
      ufOrigem: null,
      ufDestino: null,
      abrangenciaUf: null,
      origensMercadoria: ['1', '6'],
      cfops: ['6102'],
    });
  });

  it('envia alíquota e fator do percentual fixo como números', () => {
    const request = paraRegraRequest(
      valores({
        formaAliquota: 'PERCENTUAL_FIXO',
        aliquota: '6,35',
        fator: '0.25',
        prioridade: '3',
      }),
    );
    expect(request).toMatchObject({ aliquota: 6.35, fator: 0.25, prioridade: 3, ativa: true });
  });
});

describe('valoresIniciaisRegra', () => {
  it('preenche o formulário com a regra atual, com vírgula decimal', () => {
    const regra: RegraTributaria = {
      id: 1,
      nome: 'Regra',
      observacao: null,
      tributo: 'IPI',
      formaAliquota: 'ALIQUOTA_DA_NFE',
      aliquota: null,
      fator: 0.25,
      prioridade: 0,
      ativa: false,
      tipoFornecedor: 'ATACADISTA',
      fornecedorId: null,
      produtoId: 20,
      ufOrigem: null,
      ufDestino: 'GO',
      abrangenciaUf: 'INTERESTADUAL',
      origensMercadoria: ['0'],
      cfops: ['6102', '6910'],
      versao: 2,
      atualizadoEm: '2026-09-01T00:00:00Z',
    };
    expect(valoresIniciaisRegra(regra)).toMatchObject({
      fator: '0,25',
      prioridade: '0',
      ativa: 'false',
      produtoId: '20',
      origensMercadoria: '0',
      cfops: '6102,6910',
    });
  });
});
