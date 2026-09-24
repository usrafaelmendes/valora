import { describe, expect, it } from 'vitest';
import {
  formatarChaveAcesso,
  formatarCnpj,
  formatarDataHora,
  formatarMoeda,
  formatarNumero,
  formatarPercentual,
  normalizarCnpj,
} from './formatacao';

describe('formatarCnpj', () => {
  it('aplica a máscara ao CNPJ numérico', () => {
    expect(formatarCnpj('11222333000181')).toBe('11.222.333/0001-81');
  });

  it('aplica a máscara ao CNPJ alfanumérico', () => {
    expect(formatarCnpj('12ABC34501DE35')).toBe('12.ABC.345/01DE-35');
  });

  it('mantém valores fora do formato como vieram', () => {
    expect(formatarCnpj('123')).toBe('123');
  });
});

describe('normalizarCnpj', () => {
  it('remove a máscara e usa letras maiúsculas', () => {
    expect(normalizarCnpj(' 12.abc.345/01de-35 ')).toBe('12ABC34501DE35');
  });
});

/** O Intl separa "R$" do valor com espaço não separável. */
function moeda(valor: number): string {
  return formatarMoeda(valor).replace(/\s/g, ' ');
}

describe('formatação de valores da API', () => {
  it('formata moeda sem cortar casas decimais informadas', () => {
    expect(moeda(325)).toBe('R$ 325,00');
    expect(moeda(20.6375)).toBe('R$ 20,6375');
    expect(moeda(1234.5)).toBe('R$ 1.234,50');
  });

  it('formata números e percentuais como enviados', () => {
    expect(formatarNumero(0.25)).toBe('0,25');
    expect(formatarNumero(2)).toBe('2');
    expect(formatarPercentual(6.35)).toBe('6,35%');
  });

  it('formata instantes no horário de Brasília e mantém valores inválidos', () => {
    expect(formatarDataHora('2026-09-10T13:30:00Z')).toBe('10/09/2026, 10:30');
    expect(formatarDataHora('inválido')).toBe('inválido');
  });

  it('agrupa a chave de acesso em blocos de 4 dígitos', () => {
    expect(formatarChaveAcesso('1'.repeat(44))).toBe(Array(11).fill('1111').join(' '));
    expect(formatarChaveAcesso('123')).toBe('123');
  });
});
