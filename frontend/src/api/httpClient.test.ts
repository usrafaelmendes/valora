import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  autorizacaoEnviada,
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  TOKEN_TESTE,
} from '../test/fetchFalso';
import { ApiError } from './ApiError';
import { configurarHttpClient, http, nomeDoContentDisposition } from './httpClient';

let desfazer: () => void = () => undefined;

function configurar(token: string | null) {
  const aoNaoAutorizado = vi.fn();
  desfazer = configurarHttpClient({ obterToken: () => token, aoNaoAutorizado });
  return aoNaoAutorizado;
}

afterEach(() => {
  desfazer();
});

describe('http', () => {
  it('envia Authorization Bearer quando há sessão', async () => {
    configurar(TOKEN_TESTE);
    const fetchFalso = instalarFetchFalso({ 'GET /api/produtos': () => respostaJson([]) });

    await http.get('/produtos');

    expect(autorizacaoEnviada(fetchFalso.mock.calls[0]?.[1])).toBe(`Bearer ${TOKEN_TESTE}`);
  });

  it('não envia Authorization sem sessão nem em rota pública', async () => {
    configurar(TOKEN_TESTE);
    const fetchFalso = instalarFetchFalso({
      'POST /api/auth/login': () => respostaJson({}),
    });

    await http.post('/auth/login', { email: 'a', senha: 'b' }, { autenticada: false });

    expect(autorizacaoEnviada(fetchFalso.mock.calls[0]?.[1])).toBeNull();
  });

  it('envia e recebe JSON com GET, POST, PUT e DELETE', async () => {
    configurar(TOKEN_TESTE);
    const fetchFalso = instalarFetchFalso({
      'GET /api/recurso/1': () => respostaJson({ id: 1 }),
      'POST /api/recurso': () => respostaJson({ id: 2 }, 201),
      'PUT /api/recurso/2': () => respostaJson({ id: 2, nome: 'novo' }),
      'DELETE /api/recurso/2': () => new Response(null, { status: 204 }),
    });

    await expect(http.get('/recurso/1')).resolves.toEqual({ id: 1 });
    await expect(http.post('/recurso', { nome: 'x' })).resolves.toEqual({ id: 2 });
    await expect(http.put('/recurso/2', { nome: 'novo' })).resolves.toEqual({
      id: 2,
      nome: 'novo',
    });
    await expect(http.delete('/recurso/2')).resolves.toBeUndefined();

    const post = fetchFalso.mock.calls[1]?.[1];
    expect(new Headers(post?.headers).get('Content-Type')).toBe('application/json');
    expect(post?.body).toBe(JSON.stringify({ nome: 'x' }));
  });

  it('converte ProblemDetail em ApiError com mensagem e erros por campo', async () => {
    configurar(TOKEN_TESTE);
    instalarFetchFalso({
      'POST /api/recurso': () =>
        respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
          erros: { nome: 'O nome é obrigatório.' },
        }),
    });

    const erro = await http.post('/recurso', {}).catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ApiError);
    expect(erro).toMatchObject({
      status: 400,
      message: 'Dados inválidos. Corrija os campos informados.',
      erros: { nome: 'O nome é obrigatório.' },
    });
  });

  it('usa mensagem padrão quando o erro não tem corpo JSON', async () => {
    configurar(TOKEN_TESTE);
    instalarFetchFalso({ 'GET /api/recurso': () => new Response('falha', { status: 500 }) });

    await expect(http.get('/recurso')).rejects.toThrow(/erro inesperado/i);
  });

  it('avisa a sessão quando uma chamada autenticada recebe 401', async () => {
    const aoNaoAutorizado = configurar(TOKEN_TESTE);
    instalarFetchFalso({
      'GET /api/produtos': () => respostaProblema(401, 'Autenticação necessária.'),
    });

    await expect(http.get('/produtos')).rejects.toMatchObject({ status: 401 });
    expect(aoNaoAutorizado).toHaveBeenCalledOnce();
  });

  it('não trata 401 de rota pública (login inválido) como sessão expirada', async () => {
    const aoNaoAutorizado = configurar(null);
    instalarFetchFalso({
      'POST /api/auth/login': () => respostaProblema(401, 'E-mail ou senha inválidos.'),
    });

    await expect(http.post('/auth/login', {}, { autenticada: false })).rejects.toThrow(
      'E-mail ou senha inválidos.',
    );
    expect(aoNaoAutorizado).not.toHaveBeenCalled();
  });

  it('não chama aoNaoAutorizado em 403', async () => {
    const aoNaoAutorizado = configurar(TOKEN_TESTE);
    instalarFetchFalso({
      'POST /api/fornecedores': () =>
        respostaProblema(403, 'Você não tem permissão para acessar este recurso.'),
    });

    await expect(http.post('/fornecedores', {})).rejects.toMatchObject({ status: 403 });
    expect(aoNaoAutorizado).not.toHaveBeenCalled();
  });

  it('converte falha de rede em ApiError sem resposta', async () => {
    configurar(null);
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))),
    );

    const erro = await http.get('/produtos').catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ApiError);
    expect((erro as ApiError).semResposta).toBe(true);
  });

  it('envia multipart/form-data sem definir Content-Type manualmente', async () => {
    configurar(TOKEN_TESTE);
    const fetchFalso = instalarFetchFalso({ 'POST /api/nfe': () => respostaJson({ id: 1 }, 201) });
    const formData = new FormData();
    formData.append('arquivo', new Blob(['<xml/>'], { type: 'text/xml' }), 'teste.xml');

    await http.upload('/nfe', formData);

    const init = fetchFalso.mock.calls[0]?.[1];
    expect(init?.body).toBe(formData);
    expect(new Headers(init?.headers).get('Content-Type')).toBeNull();
    expect(autorizacaoEnviada(init)).toBe(`Bearer ${TOKEN_TESTE}`);
  });

  it('baixa arquivo binário com o nome informado pelo backend', async () => {
    configurar(TOKEN_TESTE);
    instalarFetchFalso({
      'GET /api/comparacoes/1/download': () =>
        new Response('a;b\n1;2\n', {
          status: 200,
          headers: {
            'Content-Type': 'text/csv;charset=UTF-8',
            'Content-Disposition': 'attachment; filename="comparacao-1.csv"',
          },
        }),
    });

    const arquivo = await http.download('/comparacoes/1/download');

    expect(arquivo.nomeArquivo).toBe('comparacao-1.csv');
    expect(await arquivo.blob.text()).toBe('a;b\n1;2\n');
  });
});

describe('nomeDoContentDisposition', () => {
  it('prioriza filename* codificado em UTF-8', () => {
    expect(
      nomeDoContentDisposition(
        'attachment; filename="cotacao.csv"; filename*=UTF-8\'\'cota%C3%A7%C3%A3o.csv',
      ),
    ).toBe('cotação.csv');
  });

  it('retorna null sem cabeçalho', () => {
    expect(nomeDoContentDisposition(null)).toBeNull();
  });
});
