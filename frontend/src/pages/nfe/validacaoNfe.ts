/**
 * Conferências locais do arquivo antes do envio, só para evitar uploads obviamente inválidos.
 * A validação definitiva (conteúdo, layout, chave, emitente) é sempre do backend.
 */

/**
 * Mesmo limite de spring.servlet.multipart.max-file-size (application.yml do backend).
 * Se o limite do backend mudar, ele continua valendo: arquivos maiores recebem 413.
 */
export const TAMANHO_MAXIMO_XML = 1024 * 1024;

/** Mensagem do problema encontrado no arquivo, ou undefined quando ele pode ser enviado. */
export function validarArquivoXml(arquivo: File | null): string | undefined {
  if (!arquivo) {
    return 'Selecione o arquivo XML da NF-e.';
  }
  if (!arquivo.name.toLowerCase().endsWith('.xml')) {
    return 'O arquivo deve ter a extensão .xml.';
  }
  if (arquivo.size === 0) {
    return 'O arquivo selecionado está vazio.';
  }
  if (arquivo.size > TAMANHO_MAXIMO_XML) {
    return 'O arquivo excede o tamanho máximo de 1 MB.';
  }
  return undefined;
}

/** Tamanho do arquivo para exibição (ex.: "12,5 KB"). */
export function formatarTamanhoArquivo(bytes: number): string {
  if (bytes < 1024) {
    return `${String(bytes)} bytes`;
  }
  const kb = bytes / 1024;
  if (kb < 1024) {
    return `${kb.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} KB`;
  }
  return `${(kb / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`;
}
