import type { ArquivoBaixado } from '../api/httpClient';

/** Oferece ao navegador o arquivo baixado pela API (ex.: CSV da comparação). */
export function salvarArquivo({ blob, nomeArquivo }: ArquivoBaixado, nomePadrao: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = nomeArquivo ?? nomePadrao;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
