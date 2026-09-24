import { Button, Group, Stack, Text } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CHAVE_FORNECEDORES, fornecedoresApi } from '../../api/fornecedores';
import { useAuth } from '../../auth/useAuth';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { ConfirmacaoAcao } from '../../components/ConfirmacaoAcao';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { NaoInformado, TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import { ROTULO_TIPO_FORNECEDOR, type Fornecedor } from '../../types/fornecedor';
import { formatarCnpj } from '../../utils/formatacao';
import { FornecedorFormulario } from './FornecedorFormulario';

/** Formulário aberto: novo cadastro (fornecedor null) ou edição. */
interface EstadoFormulario {
  fornecedor: Fornecedor | null;
}

/**
 * Fornecedores ativos (RF03). Qualquer usuário consulta; cadastro, edição e desativação
 * aparecem somente para o ADMIN (o backend também as restringe).
 */
export function FornecedoresPage() {
  const { isAdmin } = useAuth();
  const queryClient = useQueryClient();
  const [formulario, setFormulario] = useState<EstadoFormulario | null>(null);
  const [emDesativacao, setEmDesativacao] = useState<Fornecedor | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: CHAVE_FORNECEDORES,
    queryFn: ({ signal }) => fornecedoresApi.listar(signal),
  });

  const desativar = useMutation({
    mutationFn: (fornecedor: Fornecedor) => fornecedoresApi.desativar(fornecedor.id),
    onSuccess: async (_resultado, fornecedor) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_FORNECEDORES });
      setEmDesativacao(null);
      setAviso(`Fornecedor "${fornecedor.razaoSocial}" desativado.`);
    },
    onError: () => {
      // Ex.: 404 porque o fornecedor já foi desativado; a listagem é atualizada.
      void queryClient.invalidateQueries({ queryKey: CHAVE_FORNECEDORES });
    },
  });

  const abrirCadastro = () => {
    setAviso(null);
    setFormulario({ fornecedor: null });
  };

  const colunas: ColunaTabela<Fornecedor>[] = [
    {
      titulo: 'Razão social',
      conteudo: (fornecedor) => <Text fw={500}>{fornecedor.razaoSocial}</Text>,
    },
    { titulo: 'CNPJ', semQuebra: true, conteudo: (fornecedor) => formatarCnpj(fornecedor.cnpj) },
    { titulo: 'UF', conteudo: (fornecedor) => fornecedor.uf },
    { titulo: 'Tipo', conteudo: (fornecedor) => ROTULO_TIPO_FORNECEDOR[fornecedor.tipo] },
    {
      titulo: 'Prazo de pagamento base',
      conteudo: (fornecedor) => fornecedor.prazoPagamentoBase ?? <NaoInformado />,
    },
  ];
  if (isAdmin) {
    colunas.push({
      titulo: 'Ações',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (fornecedor) => (
        <Group gap="xs" justify="flex-end" wrap="nowrap">
          <Button
            size="xs"
            variant="default"
            aria-label={`Editar ${fornecedor.razaoSocial}`}
            onClick={() => {
              setAviso(null);
              setFormulario({ fornecedor });
            }}
          >
            Editar
          </Button>
          <Button
            size="xs"
            variant="subtle"
            color="red"
            aria-label={`Desativar ${fornecedor.razaoSocial}`}
            onClick={() => {
              setAviso(null);
              desativar.reset();
              setEmDesativacao(fornecedor);
            }}
          >
            Desativar
          </Button>
        </Group>
      ),
    });
  }

  return (
    <>
      <CabecalhoPagina
        titulo="Fornecedores"
        descricao={
          isAdmin
            ? 'Fornecedores ativos e os dados cadastrais usados pelas regras de cálculo configuradas.'
            : 'Fornecedores ativos disponíveis para as cotações. Somente administradores alteram este cadastro.'
        }
        acoes={isAdmin && <Button onClick={abrirCadastro}>Cadastrar fornecedor</Button>}
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
          mensagemCarregando="Carregando fornecedores…"
          tituloErro="Não foi possível carregar os fornecedores"
          vazio={
            <EstadoVazio
              titulo="Nenhum fornecedor cadastrado"
              descricao={
                isAdmin
                  ? 'Cadastre os fornecedores que participarão das cotações.'
                  : 'Ainda não há fornecedores ativos. Peça a um administrador para cadastrá-los.'
              }
              acao={isAdmin && <Button onClick={abrirCadastro}>Cadastrar fornecedor</Button>}
            />
          }
        >
          {(fornecedores) => (
            <TabelaDados
              rotulo="Fornecedores"
              itens={fornecedores}
              colunas={colunas}
              obterChave={(fornecedor) => fornecedor.id}
              larguraMinima={880}
            />
          )}
        </ResultadoConsulta>
      </Stack>

      {formulario && (
        <FornecedorFormulario
          fornecedor={formulario.fornecedor}
          aoCancelar={() => {
            setFormulario(null);
          }}
          aoSalvar={(salvo) => {
            setAviso(
              formulario.fornecedor
                ? `Fornecedor "${salvo.razaoSocial}" atualizado.`
                : `Fornecedor "${salvo.razaoSocial}" cadastrado.`,
            );
            setFormulario(null);
          }}
        />
      )}

      {emDesativacao && (
        <ConfirmacaoAcao
          titulo="Desativar fornecedor"
          rotuloConfirmar="Desativar"
          processando={desativar.isPending}
          erro={desativar.error}
          aoConfirmar={() => {
            desativar.mutate(emDesativacao);
          }}
          aoCancelar={() => {
            setEmDesativacao(null);
          }}
        >
          <Text size="sm" mb="xs">
            Deseja desativar o fornecedor <strong>{emDesativacao.razaoSocial}</strong> (CNPJ{' '}
            {formatarCnpj(emDesativacao.cnpj)})?
          </Text>
          <Text size="sm" c="dimmed">
            Ele deixará de aparecer nas listagens e não poderá mais ser editado. O registro é
            mantido para o histórico de NF-e e cotações.
          </Text>
        </ConfirmacaoAcao>
      )}
    </>
  );
}
