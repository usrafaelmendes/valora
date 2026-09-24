import { describe, expect, it } from 'vitest';
import { formatarTamanhoArquivo, TAMANHO_MAXIMO_XML, validarArquivoXml } from './validacaoNfe';

function arquivo(nome: string, tamanho: number): File {
  return new File(['x'.repeat(tamanho)], nome, { type: 'text/xml' });
}

describe('validarArquivoXml', () => {
  it('exige um arquivo selecionado', () => {
    expect(validarArquivoXml(null)).toBe('Selecione o arquivo XML da NF-e.');
  });

  it('aceita arquivo .xml (inclusive com extensão maiúscula) dentro do limite', () => {
    expect(validarArquivoXml(arquivo('nota.xml', 10))).toBeUndefined();
    expect(validarArquivoXml(arquivo('NOTA.XML', TAMANHO_MAXIMO_XML))).toBeUndefined();
  });

  it('rejeita outra extensão, arquivo vazio e arquivo acima do limite', () => {
    expect(validarArquivoXml(arquivo('nota.pdf', 10))).toBe('O arquivo deve ter a extensão .xml.');
    expect(validarArquivoXml(arquivo('nota.xml', 0))).toBe('O arquivo selecionado está vazio.');
    expect(validarArquivoXml(arquivo('nota.xml', TAMANHO_MAXIMO_XML + 1))).toBe(
      'O arquivo excede o tamanho máximo de 1 MB.',
    );
  });
});

describe('formatarTamanhoArquivo', () => {
  it('usa bytes, KB ou MB', () => {
    expect(formatarTamanhoArquivo(512)).toBe('512 bytes');
    expect(formatarTamanhoArquivo(1536)).toBe('1,5 KB');
    expect(formatarTamanhoArquivo(2 * 1024 * 1024)).toBe('2 MB');
  });
});
