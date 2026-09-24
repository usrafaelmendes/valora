import { Button, Group, Stack, Text } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CHAVE_PRODUTOS, produtosApi } from '../../api/produtos';
import { useAuth } from '../../auth/useAuth';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { ConfirmacaoAcao } from '../../components/ConfirmacaoAcao';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { NaoInformado, TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import type { Produto } from '../../types/produto';
import { ProdutoFormulario } from './ProdutoFormulario';

/** Formulário aberto: novo cadastro (produto null) ou edição. */
interface EstadoFormulario {
  produto: Produto | null;
}

/**
 * Produtos ativos (RF04). Qualquer usuário consulta; cadastro, edição e desativação
 * aparecem somente para o ADMIN (o backend também as restringe).
 */
export function ProdutosPage() {
  const { isAdmin } = useAuth();
  const queryClient = useQueryClient();
  const [formulario, setFormulario] = useState<EstadoFormulario | null>(null);
  const [emDesativacao, setEmDesativacao] = useState<Produto | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: CHAVE_PRODUTOS,
    queryFn: ({ signal }) => produtosApi.listar(signal),
  });

  const desativar = useMutation({
    mutationFn: (produto: Produto) => produtosApi.desativar(produto.id),
    onSuccess: async (_resultado, produto) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_PRODUTOS });
      setEmDesativacao(null);
      setAviso(`Produto "${produto.nome}" desativado.`);
    },
    onError: () => {
      // Ex.: 404 porque o produto já foi desativado; a listagem é atualizada.
      void queryClient.invalidateQueries({ queryKey: CHAVE_PRODUTOS });
    },
  });

  const abrirCadastro = () => {
    setAviso(null);
    setFormulario({ produto: null });
  };

  const colunas: ColunaTabela<Produto>[] = [
    { titulo: 'Nome', conteudo: (produto) => <Text fw={500}>{produto.nome}</Text> },
    {
      titulo: 'Descrição',
      conteudo: (produto) => produto.descricao ?? <NaoInformado />,
    },
    {
      titulo: 'GTIN/EAN',
      semQuebra: true,
      conteudo: (produto) => produto.gtin ?? <NaoInformado />,
    },
  ];
  if (isAdmin) {
    colunas.push({
      titulo: 'Ações',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (produto) => (
        <Group gap="xs" justify="flex-end" wrap="nowrap">
          <Button
            size="xs"
            variant="default"
            aria-label={`Editar ${produto.nome}`}
            onClick={() => {
              setAviso(null);
              setFormulario({ produto });
            }}
          >
            Editar
          </Button>
          <Button
            size="xs"
            variant="subtle"
            color="red"
            aria-label={`Desativar ${produto.nome}`}
            onClick={() => {
              setAviso(null);
              desativar.reset();
              setEmDesativacao(produto);
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
        titulo="Produtos"
        descricao={
          isAdmin
            ? 'Produtos ativos usados nas cotações e na identificação dos itens das NF-e.'
            : 'Produtos ativos disponíveis para as cotações. Somente administradores alteram este cadastro.'
        }
        acoes={isAdmin && <Button onClick={abrirCadastro}>Cadastrar produto</Button>}
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
          mensagemCarregando="Carregando produtos…"
          tituloErro="Não foi possível carregar os produtos"
          vazio={
            <EstadoVazio
              titulo="Nenhum produto cadastrado"
              descricao={
                isAdmin
                  ? 'Cadastre os produtos que serão cotados com os fornecedores.'
                  : 'Ainda não há produtos ativos. Peça a um administrador para cadastrá-los.'
              }
              acao={isAdmin && <Button onClick={abrirCadastro}>Cadastrar produto</Button>}
            />
          }
        >
          {(produtos) => (
            <TabelaDados
              rotulo="Produtos"
              itens={produtos}
              colunas={colunas}
              obterChave={(produto) => produto.id}
            />
          )}
        </ResultadoConsulta>
      </Stack>

      {formulario && (
        <ProdutoFormulario
          produto={formulario.produto}
          aoCancelar={() => {
            setFormulario(null);
          }}
          aoSalvar={(salvo) => {
            setAviso(
              formulario.produto
                ? `Produto "${salvo.nome}" atualizado.`
                : `Produto "${salvo.nome}" cadastrado.`,
            );
            setFormulario(null);
          }}
        />
      )}

      {emDesativacao && (
        <ConfirmacaoAcao
          titulo="Desativar produto"
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
            Deseja desativar o produto <strong>{emDesativacao.nome}</strong>?
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
