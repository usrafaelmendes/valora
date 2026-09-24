import { vi } from 'vitest';

/**
 * Dados fictícios, exclusivos para testes. Nenhum dado real de usuário, fornecedor ou NF-e.
 */
export const USUARIO_ADMIN_TESTE = {
  id: 1,
  nome: 'Admin Teste',
  email: 'admin@teste.local',
  perfil: 'ADMIN',
  ativo: true,
} as const;

export const USUARIO_USER_TESTE = {
  id: 2,
  nome: 'Usuário Teste',
  email: 'usuario@teste.local',
  perfil: 'USER',
  ativo: true,
} as const;

export const TOKEN_TESTE = 'token.de.teste';

/** Instante de expiração no futuro, para tokens válidos nos testes. */
export function expiraEmFuturo(horas = 8): string {
  return new Date(Date.now() + horas * 3_600_000).toISOString();
}

export function respostaJson(corpo: unknown, status = 200, tipo = 'application/json'): Response {
  return new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': tipo } });
}

export function respostaProblema(status: number, detail: string, extra: object = {}): Response {
  return respostaJson(
    { title: 'Erro', status, detail, ...extra },
    status,
    'application/problem+json',
  );
}

type Manipulador = (requisicao: { url: string; init: RequestInit }) => Response | Promise<Response>;

/** Sistema já configurado (há usuários): o padrão de todos os testes, salvo quando substituído. */
const ROTAS_PADRAO: Record<string, Manipulador> = {
  'GET /api/auth/configuracao-inicial': () => respostaJson({ configurado: true }),
};

/**
 * Substitui o fetch global. As chaves são "MÉTODO caminho" (ex.: "GET /api/auth/me");
 * chamadas sem manipulador falham o teste com 599 para ficarem evidentes.
 */
export function instalarFetchFalso(rotasDoTeste: Record<string, Manipulador>) {
  const rotas = { ...ROTAS_PADRAO, ...rotasDoTeste };
  const fetchFalso = vi.fn((entrada: RequestInfo | URL, init: RequestInit = {}) => {
    const url =
      typeof entrada === 'string' ? entrada : entrada instanceof URL ? entrada.href : entrada.url;
    const metodo = init.method ?? 'GET';
    const manipulador = rotas[`${metodo} ${url}`];
    if (!manipulador) {
      return Promise.resolve(respostaProblema(599, `Rota não simulada: ${metodo} ${url}`));
    }
    return Promise.resolve(manipulador({ url, init }));
  });
  vi.stubGlobal('fetch', fetchFalso);
  return fetchFalso;
}

/** Corpo JSON enviado em uma chamada do fetch falso. */
export function corpoEnviado(init: RequestInit): unknown {
  return typeof init.body === 'string' ? JSON.parse(init.body) : undefined;
}

/** Resposta controlada pelo teste, para observar estados de carregamento. */
export function respostaAdiada() {
  let responder: (resposta: Response) => void = () => undefined;
  const promessa = new Promise<Response>((resolver) => {
    responder = resolver;
  });
  return { promessa, responder };
}

/** Cabeçalho Authorization enviado em uma chamada do fetch falso. */
export function autorizacaoEnviada(init: RequestInit | undefined): string | null {
  return new Headers(init?.headers).get('Authorization');
}
