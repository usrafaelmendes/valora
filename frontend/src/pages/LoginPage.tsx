import {
  Alert,
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
import { useMutation } from '@tanstack/react-query';
import { useState, type SubmitEvent } from 'react';
import { Navigate, useLocation } from 'react-router';
import { mensagemDeErro } from '../api/ApiError';
import { useAuth } from '../auth/useAuth';
import {
  aguardandoServidorLocal,
  useStatusConfiguracaoInicial,
} from '../auth/useStatusConfiguracaoInicial';
import { AvisoSucesso } from '../components/AvisoSucesso';
import logoValora from '../assets/valora-logo.svg';
import { NOME_PRODUTO } from '../config/produto';
import { useAvisoNavegacao } from '../hooks/useAvisoNavegacao';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { ROTAS, type EstadoRedirecionamentoLogin } from '../routes/rotas';
import type { LoginRequest } from '../types/auth';
import classes from './LoginPage.module.css';

function destinoAposLogin(estado: unknown): string {
  const de = (estado as EstadoRedirecionamentoLogin | null)?.de;
  return de?.startsWith('/') && de !== ROTAS.login ? de : ROTAS.inicio;
}

export function LoginPage() {
  useDocumentTitle('Entrar');
  const { situacao, login, motivoFimSessao } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [aviso, fecharAviso] = useAvisoNavegacao();
  // O formulário aparece enquanto a consulta não responde; se ela falhar, o login continua possível.
  const statusConfiguracao = useStatusConfiguracaoInicial();

  const entrar = useMutation({
    mutationFn: (dados: LoginRequest) => login(dados),
  });

  // Com sessão ativa (inclusive logo após o login), segue para a página pedida.
  if (situacao === 'autenticado') {
    return <Navigate to={destinoAposLogin(location.state)} replace />;
  }
  // Instalação nova, sem nenhum usuário: primeiro é preciso criar o ADMIN.
  if (statusConfiguracao.data?.configurado === false) {
    return <Navigate to={ROTAS.configuracaoInicial} replace />;
  }

  const aoEnviar = (evento: SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault();
    entrar.mutate({ email: email.trim(), senha });
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
              Entrar
            </Title>
            <Text c="dimmed" size="sm">
              Comparação de fornecedores pelo custo efetivo de aquisição.
            </Text>
          </Stack>

          {aviso && <AvisoSucesso mensagem={aviso} aoFechar={fecharAviso} />}
          {aguardandoServidorLocal(statusConfiguracao) && (
            <Alert color="blue" variant="light" role="status">
              Aguardando o servidor do {NOME_PRODUTO}. Ao abrir o aplicativo, isso pode levar alguns
              segundos.
            </Alert>
          )}
          {motivoFimSessao === 'expirada' && !entrar.isError && (
            <Alert color="yellow" variant="light" role="status">
              Sua sessão expirou. Entre novamente para continuar.
            </Alert>
          )}
          {entrar.isError && (
            <Alert color="red" variant="light" role="alert" title="Não foi possível entrar">
              {mensagemDeErro(entrar.error)}
            </Alert>
          )}

          <form onSubmit={aoEnviar} noValidate>
            <Stack gap="md">
              <TextInput
                label="E-mail"
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(evento) => {
                  setEmail(evento.currentTarget.value);
                }}
              />
              <PasswordInput
                label="Senha"
                autoComplete="current-password"
                required
                value={senha}
                onChange={(evento) => {
                  setSenha(evento.currentTarget.value);
                }}
              />
              <Button
                type="submit"
                fullWidth
                loading={entrar.isPending}
                disabled={!email.trim() || !senha}
              >
                Entrar
              </Button>
            </Stack>
          </form>
        </Stack>
      </Paper>
    </Box>
  );
}
