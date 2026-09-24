import {
  Box,
  Button,
  Group,
  Paper,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { SubmitEvent } from 'react';
import { Navigate, useNavigate } from 'react-router';
import { ApiError } from '../../api/ApiError';
import { authApi, CHAVE_CONFIGURACAO_INICIAL } from '../../api/auth';
import { useAuth } from '../../auth/useAuth';
import { useStatusConfiguracaoInicial } from '../../auth/useStatusConfiguracaoInicial';
import { CarregandoPagina } from '../../components/CarregandoPagina';
import { MensagemErro } from '../../components/MensagemErro';
import logoValora from '../../assets/valora-logo.svg';
import { NOME_PRODUTO } from '../../config/produto';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { useFormulario } from '../../hooks/useFormulario';
import { ROTAS, type EstadoAviso } from '../../routes/rotas';
import type { ConfiguracaoInicialRequest, StatusConfiguracaoInicial } from '../../types/auth';
import classes from '../LoginPage.module.css';
import {
  paraConfiguracaoInicialRequest,
  SENHA_MAX,
  SENHA_MIN,
  validarConfiguracaoInicial,
  VALORES_INICIAIS_CONFIGURACAO,
} from './validacaoConfiguracaoInicial';

const CAMPOS = Object.keys(VALORES_INICIAIS_CONFIGURACAO);
const CONFIGURADO: StatusConfiguracaoInicial = { configurado: true };

/**
 * Primeiro acesso de uma instalação nova: cria o primeiro ADMIN. Com o sistema já configurado
 * (ou com sessão ativa), leva ao login/início. Quem decide se a criação é permitida é o backend.
 */
export function ConfiguracaoInicialPage() {
  useDocumentTitle('Configuração inicial');
  const { situacao } = useAuth();
  const status = useStatusConfiguracaoInicial();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    VALORES_INICIAIS_CONFIGURACAO,
    validarConfiguracaoInicial,
  );

  const irParaLogin = (aviso: string) => {
    queryClient.setQueryData(CHAVE_CONFIGURACAO_INICIAL, CONFIGURADO);
    const estado: EstadoAviso = { aviso };
    void navigate(ROTAS.login, { replace: true, state: estado });
  };

  const configurar = useMutation({
    mutationFn: (dados: ConfiguracaoInicialRequest) => authApi.configurarPrimeiroAdmin(dados),
    onSuccess: () => {
      irParaLogin('Administrador criado. Entre com o e-mail e a senha cadastrados.');
    },
    onError: (erro) => {
      if (erro instanceof ApiError && erro.status === 409) {
        irParaLogin(erro.message);
        return;
      }
      aplicarErrosApi(erro);
    },
  });

  if (situacao === 'autenticado') {
    return <Navigate to={ROTAS.inicio} replace />;
  }
  // Após o envio, quem navega (com o aviso) é irParaLogin; aqui só quem abriu a tela já configurado.
  if (status.data?.configurado && configurar.isIdle) {
    return <Navigate to={ROTAS.login} replace />;
  }

  const aoEnviar = (evento: SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (validarTudo()) {
      configurar.mutate(paraConfiguracaoInicialRequest(valores));
    }
  };

  return (
    <Box className={classes.pagina}>
      <Paper component="main" className={classes.painel} withBorder p="xl" radius="md">
        <Stack gap="lg">
          <Stack gap={4}>
            <Group gap="xs" wrap="nowrap">
              <img src={logoValora} alt="" className={classes.logo} />
              <Text className={classes.marca}>{NOME_PRODUTO}</Text>
            </Group>
            <Title order={1} size="h3">
              Configuração inicial
            </Title>
            <Text c="dimmed" size="sm">
              Configure o primeiro usuário administrador para começar a utilizar o sistema.
            </Text>
          </Stack>

          {status.isPending && <CarregandoPagina mensagem="Verificando configuração…" />}
          {status.isError && (
            <MensagemErro
              erro={status.error}
              titulo="Não foi possível verificar a configuração do sistema"
              aoTentarNovamente={() => void status.refetch()}
            />
          )}

          {status.data && (
            <>
              {configurar.isError && (
                <MensagemErro
                  erro={configurar.error}
                  titulo="Não foi possível criar o administrador"
                  camposOcultos={CAMPOS}
                />
              )}
              <form onSubmit={aoEnviar} noValidate>
                <Stack gap="md">
                  <TextInput
                    label="Nome"
                    autoComplete="name"
                    required
                    value={valores.nome}
                    error={erros.nome}
                    onChange={(evento) => {
                      alterar('nome', evento.currentTarget.value);
                    }}
                  />
                  <TextInput
                    label="E-mail"
                    type="email"
                    autoComplete="username"
                    required
                    value={valores.email}
                    error={erros.email}
                    onChange={(evento) => {
                      alterar('email', evento.currentTarget.value);
                    }}
                  />
                  <PasswordInput
                    label="Senha"
                    description={`Entre ${String(SENHA_MIN)} e ${String(SENHA_MAX)} caracteres.`}
                    autoComplete="new-password"
                    required
                    value={valores.senha}
                    error={erros.senha}
                    onChange={(evento) => {
                      alterar('senha', evento.currentTarget.value);
                    }}
                  />
                  <PasswordInput
                    label="Confirmar senha"
                    autoComplete="new-password"
                    required
                    value={valores.confirmacaoSenha}
                    error={erros.confirmacaoSenha}
                    onChange={(evento) => {
                      alterar('confirmacaoSenha', evento.currentTarget.value);
                    }}
                  />
                  <Button type="submit" fullWidth loading={configurar.isPending}>
                    Criar administrador
                  </Button>
                </Stack>
              </form>
            </>
          )}
        </Stack>
      </Paper>
    </Box>
  );
}
