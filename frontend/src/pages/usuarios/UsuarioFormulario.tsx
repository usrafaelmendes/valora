import { PasswordInput, Select, TextInput } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CHAVE_USUARIOS, usuariosApi } from '../../api/usuarios';
import { ROTULO_PERFIL } from '../../auth/perfil';
import { FormularioModal } from '../../components/FormularioModal';
import { useFormulario } from '../../hooks/useFormulario';
import type { Usuario } from '../../types/auth';
import type { CriarUsuarioRequest } from '../../types/usuario';
import { SENHA_MAX, SENHA_MIN } from '../configuracao-inicial/validacaoConfiguracaoInicial';
import {
  CAMPOS_USUARIO,
  ehPerfil,
  paraCriarUsuarioRequest,
  validarUsuario,
  VALORES_INICIAIS_USUARIO,
} from './validacaoUsuario';

const OPCOES_PERFIL = [
  { value: 'USER', label: `${ROTULO_PERFIL.USER} (USER)` },
  { value: 'ADMIN', label: `${ROTULO_PERFIL.ADMIN} (ADMIN)` },
];

interface UsuarioFormularioProps {
  aoSalvar: (usuario: Usuario) => void;
  aoCancelar: () => void;
}

/**
 * Cadastro de usuário (somente ADMIN). A senha só existe enquanto o formulário está aberto:
 * não é exibida depois nem devolvida pela API.
 */
export function UsuarioFormulario({ aoSalvar, aoCancelar }: UsuarioFormularioProps) {
  const queryClient = useQueryClient();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    VALORES_INICIAIS_USUARIO,
    validarUsuario,
  );

  const salvar = useMutation({
    mutationFn: (dados: CriarUsuarioRequest) => usuariosApi.criar(dados),
    onSuccess: async (salvo) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_USUARIOS });
      aoSalvar(salvo);
    },
    // E-mail já cadastrado (409) aparece no alerta do formulário, com a mensagem da API.
    onError: aplicarErrosApi,
  });

  const enviar = () => {
    if (validarTudo()) {
      salvar.mutate(paraCriarUsuarioRequest(valores));
    }
  };

  return (
    <FormularioModal
      titulo="Cadastrar usuário"
      rotuloSalvar="Cadastrar"
      salvando={salvar.isPending}
      erro={salvar.error}
      campos={CAMPOS_USUARIO}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      <TextInput
        label="Nome"
        required
        data-autofocus
        autoComplete="off"
        value={valores.nome}
        error={erros.nome}
        onChange={(evento) => {
          alterar('nome', evento.currentTarget.value);
        }}
      />
      <TextInput
        label="E-mail"
        description="Usado pelo usuário para entrar no sistema."
        type="email"
        required
        autoComplete="off"
        value={valores.email}
        error={erros.email}
        onChange={(evento) => {
          alterar('email', evento.currentTarget.value);
        }}
      />
      <PasswordInput
        label="Senha"
        description={`Entre ${String(SENHA_MIN)} e ${String(SENHA_MAX)} caracteres.`}
        required
        autoComplete="new-password"
        value={valores.senha}
        error={erros.senha}
        onChange={(evento) => {
          alterar('senha', evento.currentTarget.value);
        }}
      />
      <PasswordInput
        label="Confirmar senha"
        required
        autoComplete="new-password"
        value={valores.confirmacaoSenha}
        error={erros.confirmacaoSenha}
        onChange={(evento) => {
          alterar('confirmacaoSenha', evento.currentTarget.value);
        }}
      />
      <Select
        label="Perfil"
        description="Administradores gerenciam cadastros, NF-e, regras e usuários."
        placeholder="Selecione"
        required
        data={OPCOES_PERFIL}
        value={valores.perfil || null}
        error={erros.perfil}
        onChange={(valor) => {
          alterar('perfil', ehPerfil(valor) ? valor : '');
        }}
      />
    </FormularioModal>
  );
}
