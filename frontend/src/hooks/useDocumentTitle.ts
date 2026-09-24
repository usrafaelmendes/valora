import { useEffect } from 'react';
import { NOME_PRODUTO } from '../config/produto';

/** Atualiza o título da aba, que também é anunciado por leitores de tela ao trocar de página. */
export function useDocumentTitle(titulo: string): void {
  useEffect(() => {
    document.title = `${titulo} | ${NOME_PRODUTO}`;
  }, [titulo]);
}
