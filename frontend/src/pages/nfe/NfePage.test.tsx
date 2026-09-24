import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { arquivoXmlTeste, NFE_RESUMO_TESTE, NFE_TESTE } from '../../test/dadosFicticios';
import {
  instalarFetchFalso,
  respostaAdiada,
  respostaJson,
  respostaProblema,
  USUARIO_ADMIN_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';
import { salvarSessaoTeste } from '../../test/sessao';

function chamadas(fetchFalso: ReturnType<typeof instalarFetchFalso>, metodo: string) {
  return fetchFalso.mock.calls.filter(([, init]) => (init?.method ?? 'GET') === metodo);
}

function entradaArquivo(modal: HTMLElement): HTMLInputElement {
  const entrada = modal.querySelector<HTMLInputElement>('input[type="file"]');
  if (!entrada) {
    throw new Error('Campo de arquivo não encontrado.');
  }
  return entrada;
}

async function abrirImportacao() {
  const usuario = userEvent.setup({ applyAccept: false });
  await usuario.click(await screen.findByRole('button', { name: 'Importar NF-e' }));
  const modal = await screen.findByRole('dialog', { name: 'Importar NF-e' });
  return { usuario, modal };
}

describe('NfePage — listagem', () => {
  it('lista as NF-e importadas com os dados do backend', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([NFE_RESUMO_TESTE]),
    });
    renderizarApp('/nfe');

    const tabela = await screen.findByRole('table', { name: 'NF-e importadas' });
    const linha = within(tabela).getAllByRole('row')[1];
    expect(linha).toHaveTextContent('Nº 123 / 1');
    expect(linha).toHaveTextContent('10/09/2026, 10:30');
    expect(linha).toHaveTextContent('Fornecedor Ficticio Ltda');
    expect(linha).toHaveTextContent('Venda de mercadoria (teste)');
    expect(linha).toHaveTextContent('R$ 325,00');
    expect(within(tabela).getByRole('link', { name: 'Ver detalhes da NF-e 123' })).toHaveAttribute(
      'href',
      '/nfe/1',
    );
  });

  it('mostra o carregamento e depois o estado vazio', async () => {
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => adiada.promessa,
    });
    renderizarApp('/nfe');

    expect(await screen.findByText('Carregando NF-e…')).toBeInTheDocument();
    adiada.responder(respostaJson([]));

    expect(await screen.findByText('Nenhuma NF-e importada')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Importar NF-e' })).toHaveLength(2);
  });

  it('mostra o erro da API e permite tentar novamente', async () => {
    let falhar = true;
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () =>
        falhar
          ? respostaProblema(500, 'Ocorreu um erro interno.')
          : respostaJson([NFE_RESUMO_TESTE]),
    });
    renderizarApp('/nfe');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('Não foi possível carregar as NF-e');
    falhar = false;
    await userEvent.setup().click(within(alerta).getByRole('button', { name: 'Tentar novamente' }));

    expect(await screen.findByRole('table', { name: 'NF-e importadas' })).toBeInTheDocument();
  });

  it('mostra a mensagem do backend quando o acesso é negado (403)', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });
    renderizarApp('/nfe');

    expect(await screen.findByRole('alert', {}, { timeout: 3000 })).toHaveTextContent(
      'Você não tem permissão para acessar este recurso.',
    );
  });
});

describe('NfePage — importação', () => {
  it('envia o XML como multipart, mostra o processamento e o sucesso', async () => {
    let nfes: unknown[] = [];
    const adiada = respostaAdiada();
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson(nfes),
      'POST /api/nfe': () => adiada.promessa,
    });
    renderizarApp('/nfe');
    const { usuario, modal } = await abrirImportacao();

    const arquivo = arquivoXmlTeste();
    await usuario.upload(entradaArquivo(modal), arquivo);
    expect(modal).toHaveTextContent('Arquivo selecionado: nfe-ficticia.xml');

    await usuario.click(within(modal).getByRole('button', { name: 'Importar' }));

    // Enquanto processa: indicador visível, envio e cancelamento bloqueados.
    expect(await within(modal).findByText('Enviando e processando o XML…')).toBeInTheDocument();
    expect(within(modal).getByRole('button', { name: 'Cancelar' })).toBeDisabled();
    await usuario.click(within(modal).getByRole('button', { name: /Importar/ }));
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(1);

    const envio = chamadas(fetchFalso, 'POST')[0]?.[1];
    expect(envio?.body).toBeInstanceOf(FormData);
    expect((envio?.body as FormData).get('arquivo')).toBe(arquivo);

    nfes = [NFE_RESUMO_TESTE];
    adiada.responder(respostaJson(NFE_TESTE, 201));

    const aviso = await screen.findByRole('status');
    expect(aviso).toHaveTextContent('NF-e nº 123 importada com 2 itens (1 sem produto vinculado).');
    expect(within(aviso).getByRole('link', { name: 'Ver detalhes' })).toHaveAttribute(
      'href',
      '/nfe/1',
    );
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(await screen.findByRole('table', { name: 'NF-e importadas' })).toHaveTextContent(
      'Nº 123 / 1',
    );
  });

  it('não envia sem arquivo nem com extensão inválida', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([]),
    });
    renderizarApp('/nfe');
    const { usuario, modal } = await abrirImportacao();

    await usuario.click(within(modal).getByRole('button', { name: 'Importar' }));
    expect(within(modal).getByText('Selecione o arquivo XML da NF-e.')).toBeInTheDocument();

    await usuario.upload(entradaArquivo(modal), arquivoXmlTeste('nota.pdf'));
    await usuario.click(within(modal).getByRole('button', { name: 'Importar' }));
    expect(within(modal).getByText('O arquivo deve ter a extensão .xml.')).toBeInTheDocument();

    await usuario.upload(entradaArquivo(modal), arquivoXmlTeste('vazia.xml', ''));
    await usuario.click(within(modal).getByRole('button', { name: 'Importar' }));
    expect(within(modal).getByText('O arquivo selecionado está vazio.')).toBeInTheDocument();

    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);
  });

  it('permite remover o arquivo selecionado e cancelar sem enviar', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([]),
    });
    renderizarApp('/nfe');
    const { usuario, modal } = await abrirImportacao();

    await usuario.upload(entradaArquivo(modal), arquivoXmlTeste());
    expect(modal).toHaveTextContent('nfe-ficticia.xml');
    await usuario.click(within(modal).getByRole('button', { name: 'Remover arquivo selecionado' }));
    expect(modal).not.toHaveTextContent('Arquivo selecionado');

    await usuario.click(within(modal).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    expect(chamadas(fetchFalso, 'POST')).toHaveLength(0);
  });

  it.each([
    [400, 'O arquivo enviado não é um XML válido.'],
    [409, 'Esta NF-e (mesma chave de acesso) já foi importada.'],
    [413, 'O arquivo enviado excede o tamanho máximo permitido.'],
    [415, 'Content-Type não suportado.'],
    [
      422,
      'O emitente da NF-e (CNPJ 11222333000181) não está cadastrado como fornecedor. Cadastre o fornecedor e importe a NF-e novamente.',
    ],
  ])('mostra o erro %i devolvido pelo backend e mantém o formulário', async (status, detalhe) => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([]),
      'POST /api/nfe': () => respostaProblema(status, detalhe),
    });
    renderizarApp('/nfe');
    const { usuario, modal } = await abrirImportacao();

    await usuario.upload(entradaArquivo(modal), arquivoXmlTeste());
    await usuario.click(within(modal).getByRole('button', { name: 'Importar' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent(detalhe);
    expect(within(modal).getByRole('button', { name: 'Importar' })).toBeEnabled();

    // Trocar o arquivo limpa o erro anterior.
    await usuario.upload(entradaArquivo(modal), arquivoXmlTeste('outra.xml'));
    expect(within(modal).queryByRole('alert')).not.toBeInTheDocument();
  });

  it('usa a mensagem padrão quando o 413 vem sem corpo', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([]),
      'POST /api/nfe': () => new Response(null, { status: 413 }),
    });
    renderizarApp('/nfe');
    const { usuario, modal } = await abrirImportacao();

    await usuario.upload(entradaArquivo(modal), arquivoXmlTeste());
    await usuario.click(within(modal).getByRole('button', { name: 'Importar' }));

    expect(await within(modal).findByRole('alert')).toHaveTextContent(
      'O arquivo enviado excede o tamanho máximo permitido.',
    );
  });
});

describe('NfeDetalhePage', () => {
  it('mostra identificação, totais e itens exatamente como vieram do backend', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe/1': () => respostaJson(NFE_TESTE),
    });
    renderizarApp('/nfe/1');

    expect(
      await screen.findByRole('heading', { name: 'NF-e nº 123 (série 1)' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('3126 0911 2223 3300 0181 5500 1000 0001 2310 0000 1234'),
    ).toBeInTheDocument();
    expect(screen.getByText('11.222.333/0001-81')).toBeInTheDocument();
    expect(screen.getByText('R$ 325,00')).toBeInTheDocument();
    // Destinatário sem CNPJ na nota: nada é presumido.
    expect(screen.getAllByText('Não informado').length).toBeGreaterThan(0);
    expect(
      screen.getByText(/1 item não está vinculado a um produto cadastrado/),
    ).toBeInTheDocument();

    const usuario = userEvent.setup();
    const item1 = screen.getByRole('button', { name: 'Item 1: Item ficticio A' });
    expect(item1).toHaveTextContent('Produto Ficticio A');
    expect(screen.getByRole('button', { name: 'Item 2: Cabo ficticio' })).toHaveTextContent(
      'Sem produto vinculado',
    );

    await usuario.click(item1);
    const tributos = await screen.findByRole('table', { name: 'Tributos destacados no item 1' });
    const linhas = within(tributos).getAllByRole('row');
    expect(linhas[1]).toHaveTextContent(/ICMS\s*1\s*00\s*R\$ 100,00\s*7%\s*R\$ 7,00/);
    expect(linhas[2]).toHaveTextContent(/IPI.*50.*15%.*R\$ 15,00/);
    expect(linhas[3]).toHaveTextContent('1,35%');
    expect(linhas[4]).toHaveTextContent('5%');
    // Valor unitário com todas as casas enviadas, sem arredondamento.
    expect(screen.getByText('R$ 50,1234567891')).toBeInTheDocument();
    expect(screen.getByText(/Origem 1 \(tag orig\)/)).toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Item 2: Cabo ficticio' }));
    const tributos2 = await screen.findByRole('table', { name: 'Tributos destacados no item 2' });
    expect(within(tributos2).getAllByRole('row')[1]).toHaveTextContent('CSOSN 102');
    expect(within(tributos2).getAllByRole('row')[2]).toHaveTextContent('Não informado');
  });

  it('mostra o erro quando a NF-e não existe', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe/99': () => respostaProblema(404, 'NF-e não encontrada.'),
    });
    renderizarApp('/nfe/99');

    const alerta = await screen.findByRole('alert', {}, { timeout: 3000 });
    expect(alerta).toHaveTextContent('NF-e não encontrada.');
    expect(screen.getByRole('link', { name: 'Voltar para NF-e' })).toHaveAttribute('href', '/nfe');
  });

  it('não consulta a API com id inválido', async () => {
    salvarSessaoTeste();
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
    });
    renderizarApp('/nfe/abc');

    expect(await screen.findByRole('heading', { name: 'NF-e não encontrada' })).toBeInTheDocument();
    expect(fetchFalso).toHaveBeenCalledTimes(1);
  });

  it('navega da listagem para os detalhes', async () => {
    salvarSessaoTeste();
    instalarFetchFalso({
      'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE),
      'GET /api/nfe': () => respostaJson([NFE_RESUMO_TESTE]),
      'GET /api/nfe/1': () => respostaJson(NFE_TESTE),
    });
    renderizarApp('/nfe');

    await userEvent.setup().click(await screen.findByRole('link', { name: 'Nº 123 / 1' }));

    expect(
      await screen.findByRole('heading', { name: 'NF-e nº 123 (série 1)' }),
    ).toBeInTheDocument();
    // O menu continua indicando a área de NF-e nas subpáginas.
    expect(
      within(screen.getByRole('navigation', { name: 'Menu principal' })).getByRole('link', {
        name: 'NF-e',
      }),
    ).toHaveAttribute('aria-current', 'page');
  });
});
