import { Badge, Button, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { CHAVE_USUARIOS, usuariosApi } from '../../api/usuarios';
import { ROTULO_PERFIL } from '../../auth/perfil';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import type { Usuario } from '../../types/auth';
import { UsuarioFormulario } from './UsuarioFormulario';

const COLUNAS: ColunaTabela<Usuario>[] = [
  { titulo: 'Nome', conteudo: (usuario) => <Text fw={500}>{usuario.nome}</Text> },
  { titulo: 'E-mail', conteudo: (usuario) => usuario.email },
  { titulo: 'Perfil', semQuebra: true, conteudo: (usuario) => ROTULO_PERFIL[usuario.perfil] },
  {
    titulo: 'Situação',
    semQuebra: true,
    conteudo: (usuario) =>
      usuario.ativo ? (
        <Badge color="teal" variant="light">
          Ativo
        </Badge>
      ) : (
        <Badge color="gray" variant="light">
          Inativo
        </Badge>
      ),
  },
];

/**
 * Usuários do sistema (somente ADMIN; o backend também restringe /usuarios). O primeiro ADMIN
 * continua sendo criado pela configuração inicial; esta tela cadastra os demais.
 */
export function UsuariosPage() {
  const [formularioAberto, setFormularioAberto] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: CHAVE_USUARIOS,
    queryFn: ({ signal }) => usuariosApi.listar(signal),
  });

  const abrirCadastro = () => {
    setAviso(null);
    setFormularioAberto(true);
  };

  return (
    <>
      <CabecalhoPagina
        titulo="Usuários"
        descricao="Pessoas com acesso ao sistema. Usuários comuns consultam cadastros e fazem cotações; administradores também gerenciam cadastros, NF-e, regras e usuários."
        acoes={<Button onClick={abrirCadastro}>Cadastrar usuário</Button>}
      />

      <Stack gap="md">
        {aviso && (
          <AvisoSucesso
            mensagem={aviso}
            aoFechar={() => {
              setAviso(null);
            }}
          />
        )}

        <ResultadoConsulta
          consulta={consulta}
          mensagemCarregando="Carregando usuários…"
          tituloErro="Não foi possível carregar os usuários"
          vazio={
            <EstadoVazio
              titulo="Nenhum usuário encontrado"
              descricao="Cadastre as pessoas que vão acessar o sistema."
              acao={<Button onClick={abrirCadastro}>Cadastrar usuário</Button>}
            />
          }
        >
          {(usuarios) => (
            <TabelaDados
              rotulo="Usuários"
              itens={usuarios}
              colunas={COLUNAS}
              obterChave={(usuario) => usuario.id}
              larguraMinima={640}
            />
          )}
        </ResultadoConsulta>
      </Stack>

      {formularioAberto && (
        <UsuarioFormulario
          aoCancelar={() => {
            setFormularioAberto(false);
          }}
          aoSalvar={(salvo) => {
            setAviso(
              `Usuário "${salvo.nome}" cadastrado com o perfil ${ROTULO_PERFIL[salvo.perfil]}.`,
            );
            setFormularioAberto(false);
          }}
        />
      )}
    </>
  );
}
