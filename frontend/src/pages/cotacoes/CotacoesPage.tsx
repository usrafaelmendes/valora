import { Anchor, Button, Group, Select, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { chaveListaCotacoes, cotacoesApi } from '../../api/cotacoes';
import { CHAVE_PRODUTOS, produtosApi } from '../../api/produtos';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { NaoInformado, TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import { caminhoCotacao, ROTAS } from '../../routes/rotas';
import type { CotacaoResumo } from '../../types/cotacao';
import { formatarDataHora, formatarNumero } from '../../utils/formatacao';

/**
 * Cotações (RF08, RF12), da mais recente para a mais antiga, na ordem devolvida pelo
 * backend. Qualquer usuário autenticado cria cotações e executa comparações.
 */
export function CotacoesPage() {
  const [produtoId, setProdutoId] = useState<string | null>(null);
  const filtro = produtoId === null ? null : Number(produtoId);

  const consulta = useQuery({
    queryKey: chaveListaCotacoes(filtro),
    queryFn: ({ signal }) => cotacoesApi.listar(filtro, signal),
  });
  const produtos = useQuery({
    queryKey: CHAVE_PRODUTOS,
    queryFn: ({ signal }) => produtosApi.listar(signal),
  });

  const novaCotacao = (
    <Button component={Link} to={ROTAS.novaCotacao}>
      Nova cotação
    </Button>
  );

  const colunas: ColunaTabela<CotacaoResumo>[] = [
    {
      titulo: 'Cotação',
      semQuebra: true,
      conteudo: (cotacao) => (
        <Anchor component={Link} to={caminhoCotacao(cotacao.id)} fw={500}>
          {`Nº ${String(cotacao.id)}`}
        </Anchor>
      ),
    },
    { titulo: 'Produto', conteudo: (cotacao) => cotacao.produtoNome },
    {
      titulo: 'Quantidade',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (cotacao) => formatarNumero(cotacao.quantidade),
    },
    { titulo: 'Descrição', conteudo: (cotacao) => cotacao.descricao ?? <NaoInformado /> },
    {
      titulo: 'Criada em',
      semQuebra: true,
      conteudo: (cotacao) => formatarDataHora(cotacao.criadoEm),
    },
    {
      titulo: 'Ações',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (cotacao) => (
        <Button
          component={Link}
          to={caminhoCotacao(cotacao.id)}
          size="xs"
          variant="default"
          aria-label={`Ver detalhes da cotação ${String(cotacao.id)}`}
        >
          Detalhes
        </Button>
      ),
    },
  ];

  return (
    <>
      <CabecalhoPagina
        titulo="Cotações"
        descricao="Informe o produto e as opções de fornecedores: o sistema calcula o custo efetivo de cada opção, com as regras tributárias configuradas, e apresenta a comparação."
        acoes={novaCotacao}
      />

      <Stack gap="md">
        <Group>
          <Select
            label="Filtrar por produto"
            placeholder="Todos os produtos"
            searchable
            clearable
            w={320}
            data={(produtos.data ?? []).map((produto) => ({
              value: String(produto.id),
              label: produto.nome,
            }))}
            value={produtoId}
            onChange={setProdutoId}
          />
        </Group>

        <ResultadoConsulta
          consulta={consulta}
          mensagemCarregando="Carregando cotações…"
          tituloErro="Não foi possível carregar as cotações"
          vazio={
            filtro === null ? (
              <EstadoVazio
                titulo="Nenhuma cotação registrada"
                descricao="Crie uma cotação com o produto e as opções de fornecedores para comparar o custo efetivo."
                acao={novaCotacao}
              />
            ) : (
              <EstadoVazio
                titulo="Nenhuma cotação para este produto"
                descricao="Limpe o filtro para ver todas as cotações."
              />
            )
          }
        >
          {(cotacoes) => (
            <TabelaDados
              rotulo="Cotações registradas"
              itens={cotacoes}
              colunas={colunas}
              obterChave={(cotacao) => cotacao.id}
              larguraMinima={760}
            />
          )}
        </ResultadoConsulta>

        <Text c="dimmed" size="xs">
          Cotações e comparações são históricas: não são alteradas nem apagadas. Cada nova
          comparação gera um novo registro.
        </Text>
      </Stack>
    </>
  );
}
