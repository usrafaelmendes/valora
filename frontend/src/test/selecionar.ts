import { waitFor, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

/**
 * Escolhe uma opção de Select/MultiSelect na lista controlada pelo próprio campo (há outros
 * campos com as mesmas opções na página). No jsdom a lista abre em transição, ainda oculta.
 */
export async function selecionar(
  usuario: UserEvent,
  container: HTMLElement,
  campo: RegExp,
  opcao: string,
) {
  const entrada = within(container).getByLabelText(campo, { selector: 'input' });
  const listaAberta = (id: string | null) => (id ? document.getElementById(id) : null);
  // MultiSelect continua aberto após uma escolha: clicar de novo o fecharia.
  if (!listaAberta(entrada.getAttribute('aria-controls'))) {
    await usuario.click(entrada);
  }
  const lista = await waitFor(() => {
    const elemento = listaAberta(entrada.getAttribute('aria-controls'));
    if (!elemento) {
      throw new Error(`Lista de opções de ${String(campo)} não encontrada.`);
    }
    return elemento;
  });
  await usuario.click(within(lista).getByRole('option', { name: opcao, hidden: true }));
}
