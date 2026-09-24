import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { tokenStorage } from '../../auth/tokenStorage';
import {
  corpoEnviado,
  expiraEmFuturo,
  instalarFetchFalso,
  respostaJson,
  respostaProblema,
  TOKEN_TESTE,
  USUARIO_ADMIN_TESTE,
} from '../../test/fetchFalso';
import { renderizarApp } from '../../test/renderizar';

const NAO_CONFIGURADO = {
  'GET /api/auth/configuracao-inicial': () => respostaJson({ configurado: false }),
};

async function preencher(dados: {
  nome: string;
  email: string;
  senha: string;
  confirmacao: string;
}) {
  const usuario = userEvent.setup();
  await usuario.type(await screen.findByLabelText(/^Nome/), dados.nome);
  await usuario.type(screen.getByLabelText(/^E-mail/), dados.email);
  await usuario.type(screen.getByLabelText(/^Senha/), dados.senha);
  await usuario.type(screen.getByLabelText(/^Confirmar senha/), dados.confirmacao);
  await usuario.click(screen.getByRole('button', { name: 'Criar administrador' }));
}

const DADOS_VALIDOS = {
  nome: 'Admin Teste',
  email: 'admin@teste.local',
  senha: 'senha-de-teste',
  confirmacao: 'senha-de-teste',
};

describe('configuração inicial', () => {
  it('sem usuários, a aplicação leva do login à tela de configuração inicial', async () => {
    instalarFetchFalso(NAO_CONFIGURADO);

    renderizarApp('/');

    expect(
      await screen.findByRole('heading', { name: 'Configuração inicial' }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/^Nome/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Confirmar senha/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Entrar' })).not.toBeInTheDocument();
  });

  it('cria o ADMIN e vai ao login com aviso de sucesso, sem voltar à configuração', async () => {
    let configurado = false;
    const fetchFalso = instalarFetchFalso({
      'GET /api/auth/configuracao-inicial': () => respostaJson({ configurado }),
      'POST /api/auth/configuracao-inicial': () => {
        configurado = true;
        return respostaJson(USUARIO_ADMIN_TESTE, 201);
      },
    });
    renderizarApp('/configuracao-inicial');

    await preencher({ ...DADOS_VALIDOS, nome: '  Admin Teste ' });

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    expect(
      screen.getByText('Administrador criado. Entre com o e-mail e a senha cadastrados.'),
    ).toBeInTheDocument();
    const envio = fetchFalso.mock.calls.find(([, init]) => init?.method === 'POST')?.[1];
    expect(corpoEnviado(envio ?? {})).toEqual({
      nome: 'Admin Teste',
      email: 'admin@teste.local',
      senha: 'senha-de-teste',
      confirmacaoSenha: 'senha-de-teste',
    });
    // A criação não inicia sessão: o ADMIN entra pelo login.
    expect(tokenStorage.ler()).toBeNull();
    expect(screen.getByLabelText(/e-mail/i)).toHaveValue('');
  });

  it('valida os campos antes de enviar', async () => {
    const fetchFalso = instalarFetchFalso(NAO_CONFIGURADO);
    renderizarApp('/configuracao-inicial');

    await userEvent
      .setup()
      .click(await screen.findByRole('button', { name: 'Criar administrador' }));

    expect(screen.getByText('O nome é obrigatório.')).toBeInTheDocument();
    expect(screen.getByText('O e-mail é obrigatório.')).toBeInTheDocument();
    expect(screen.getByText('A senha é obrigatória.')).toBeInTheDocument();
    expect(screen.getByText('A confirmação da senha é obrigatória.')).toBeInTheDocument();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('não envia com e-mail inválido ou senha curta', async () => {
    const fetchFalso = instalarFetchFalso(NAO_CONFIGURADO);
    renderizarApp('/configuracao-inicial');

    await preencher({
      ...DADOS_VALIDOS,
      email: 'nao-e-email',
      senha: 'curta',
      confirmacao: 'curta',
    });

    expect(screen.getByText('O e-mail informado é inválido.')).toBeInTheDocument();
    expect(screen.getByText('A senha deve ter entre 8 e 72 caracteres.')).toBeInTheDocument();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('não envia quando as senhas são diferentes', async () => {
    const fetchFalso = instalarFetchFalso(NAO_CONFIGURADO);
    renderizarApp('/configuracao-inicial');

    await preencher({ ...DADOS_VALIDOS, confirmacao: 'outra-senha-123' });

    expect(screen.getByText('A confirmação da senha não confere.')).toBeInTheDocument();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('mostra os erros de validação devolvidos pela API nos campos', async () => {
    instalarFetchFalso({
      ...NAO_CONFIGURADO,
      'POST /api/auth/configuracao-inicial': () =>
        respostaProblema(400, 'Dados inválidos. Corrija os campos informados.', {
          erros: { email: 'O e-mail informado é inválido.' },
        }),
    });
    renderizarApp('/configuracao-inicial');

    await preencher({ ...DADOS_VALIDOS, email: 'admin@teste' });

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Dados inválidos. Corrija os campos informados.',
    );
    expect(screen.getByText('O e-mail informado é inválido.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Configuração inicial' })).toBeInTheDocument();
  });

  it('mostra erro de comunicação com a API e mantém os dados digitados', async () => {
    instalarFetchFalso({
      ...NAO_CONFIGURADO,
      'POST /api/auth/configuracao-inicial': () =>
        respostaProblema(
          500,
          'Ocorreu um erro interno. Tente novamente ou contate o administrador.',
        ),
    });
    renderizarApp('/configuracao-inicial');

    await preencher(DADOS_VALIDOS);

    expect(await screen.findByRole('alert')).toHaveTextContent('Ocorreu um erro interno.');
    expect(screen.getByLabelText(/^Nome/)).toHaveValue('Admin Teste');
  });

  it('se outro acesso configurou o sistema antes (409), leva ao login com o aviso', async () => {
    instalarFetchFalso({
      ...NAO_CONFIGURADO,
      'POST /api/auth/configuracao-inicial': () =>
        respostaProblema(409, 'O sistema já foi configurado. Entre com um usuário existente.'),
    });
    renderizarApp('/configuracao-inicial');

    await preencher(DADOS_VALIDOS);

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    expect(
      screen.getByText('O sistema já foi configurado. Entre com um usuário existente.'),
    ).toBeInTheDocument();
  });

  it('com o sistema já configurado, a tela de configuração leva ao login', async () => {
    instalarFetchFalso({});

    renderizarApp('/configuracao-inicial');

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Configuração inicial' })).not.toBeInTheDocument();
  });

  it('com sessão ativa, a tela de configuração leva ao início', async () => {
    tokenStorage.salvar({ token: TOKEN_TESTE, expiraEm: expiraEmFuturo() });
    instalarFetchFalso({ 'GET /api/auth/me': () => respostaJson(USUARIO_ADMIN_TESTE) });

    renderizarApp('/configuracao-inicial');

    expect(await screen.findByText('Olá, Admin Teste')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Configuração inicial' })).not.toBeInTheDocument();
  });

  it('enquanto o servidor não responde, o login avisa que aguarda e segue quando ele sobe', async () => {
    // Simula o backend local ainda iniciando (desktop): as primeiras conexões são recusadas.
    let tentativas = 0;
    instalarFetchFalso({
      'GET /api/auth/configuracao-inicial': () => {
        tentativas += 1;
        if (tentativas <= 2) {
          throw new TypeError('Failed to fetch');
        }
        return respostaJson({ configurado: false });
      },
    });

    renderizarApp('/login');

    expect(await screen.findByRole('status')).toHaveTextContent('Aguardando o servidor do Valora');
    expect(
      await screen.findByRole('heading', { name: 'Configuração inicial' }, { timeout: 5_000 }),
    ).toBeInTheDocument();
    expect(tentativas).toBe(3);
  });

  it('falha ao verificar a configuração mostra erro com opção de tentar novamente', async () => {
    instalarFetchFalso({
      'GET /api/auth/configuracao-inicial': () => respostaProblema(503, 'Serviço indisponível.'),
    });

    renderizarApp('/configuracao-inicial');

    expect(await screen.findByRole('alert')).toHaveTextContent('Serviço indisponível.');
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Criar administrador' })).not.toBeInTheDocument();
  });
});
