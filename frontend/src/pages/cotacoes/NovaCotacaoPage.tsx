import {
  Button,
  Group,
  Paper,
  Select,
  SimpleGrid,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRef, useState, type SubmitEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '../../api/ApiError';
import { chaveComparacao } from '../../api/comparacoes';
import { CHAVE_COTACOES, cotacoesApi } from '../../api/cotacoes';
import { CHAVE_PRODUTOS, produtosApi } from '../../api/produtos';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { MensagemErro } from '../../components/MensagemErro';
import { useFormulario, type ErrosFormulario } from '../../hooks/useFormulario';
import { caminhoComparacao, caminhoCotacao, ROTAS, type EstadoAviso } from '../../routes/rotas';
import { MAXIMO_OPCOES, type CotacaoRequest } from '../../types/cotacao';
import { CamposOpcao } from './CamposOpcao';
import {
  CAMPOS_COTACAO,
  camposDasOpcoes,
  errosDaOpcao,
  paraCotacaoRequest,
  semErros,
  validarCotacao,
  validarOpcao,
  VALORES_INICIAIS_COTACAO,
  valoresIniciaisOpcao,
  type ValoresOpcao,
} from './validacaoCotacao';

interface OpcaoEmEdicao {
  /** Identificador local, estável ao remover opções do meio da lista. */
  chave: number;
  valores: ValoresOpcao;
  erros: ErrosFormulario<ValoresOpcao>;
}

/**
 * Nova cotação (POST /cotacoes, CT23): produto, quantidade, descrição e de 1 a 50 opções.
 * O backend cria a cotação e já executa a primeira comparação, que é aberta em seguida.
 */
export function NovaCotacaoPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const proximaChave = useRef(1);
  const [opcoes, setOpcoes] = useState<OpcaoEmEdicao[]>([
    { chave: 0, valores: valoresIniciaisOpcao(), erros: {} },
  ]);
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    VALORES_INICIAIS_COTACAO,
    validarCotacao,
  );

  const produtos = useQuery({
    queryKey: CHAVE_PRODUTOS,
    queryFn: ({ signal }) => produtosApi.listar(signal),
  });

  const criar = useMutation({
    mutationFn: (dados: CotacaoRequest) => cotacoesApi.criar(dados),
    onSuccess: async (cotacao) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_COTACOES });
      const comparacao = cotacao.ultimaComparacao;
      if (comparacao) {
        queryClient.setQueryData(chaveComparacao(comparacao.id), comparacao);
      }
      const estado: EstadoAviso = {
        aviso: comparacao
          ? `Cotação nº ${String(cotacao.id)} criada. Resultado da comparação inicial nº ${String(comparacao.id)}.`
          : `Cotação nº ${String(cotacao.id)} criada.`,
      };
      void navigate(comparacao ? caminhoComparacao(comparacao.id) : caminhoCotacao(cotacao.id), {
        state: estado,
      });
    },
    onError: (erro) => {
      aplicarErrosApi(erro);
      if (erro instanceof ApiError) {
        setOpcoes((atuais) =>
          atuais.map((opcao, indice) => ({
            ...opcao,
            erros: { ...opcao.erros, ...errosDaOpcao(erro.erros, indice) },
          })),
        );
      }
    },
  });

  const alterarOpcao = (chave: number, campo: keyof ValoresOpcao, valor: string) => {
    setOpcoes((atuais) =>
      atuais.map((opcao) =>
        opcao.chave === chave
          ? {
              ...opcao,
              valores: { ...opcao.valores, [campo]: valor },
              erros: { ...opcao.erros, [campo]: undefined },
            }
          : opcao,
      ),
    );
  };

  const incluirOpcao = () => {
    const chave = proximaChave.current;
    proximaChave.current += 1;
    setOpcoes((atuais) => [...atuais, { chave, valores: valoresIniciaisOpcao(), erros: {} }]);
  };

  const removerOpcao = (chave: number) => {
    setOpcoes((atuais) => atuais.filter((opcao) => opcao.chave !== chave));
  };

  const enviar = (evento: SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const cotacaoValida = validarTudo();
    const validadas = opcoes.map((opcao) => ({ ...opcao, erros: validarOpcao(opcao.valores) }));
    setOpcoes(validadas);
    if (cotacaoValida && validadas.every((opcao) => semErros(opcao.erros))) {
      criar.mutate(
        paraCotacaoRequest(
          valores,
          opcoes.map((opcao) => opcao.valores),
        ),
      );
    }
  };

  const produtoId = valores.produtoId ? Number(valores.produtoId) : null;

  return (
    <>
      <CabecalhoPagina
        titulo="Nova cotação"
        descricao="Ao salvar, o sistema cria a cotação e executa a primeira comparação com as regras e os parâmetros vigentes."
        acoes={
          <Button component={Link} to={ROTAS.cotacoes} variant="default" size="xs">
            Voltar para cotações
          </Button>
        }
      />

      <form onSubmit={enviar} noValidate aria-label="Nova cotação">
        <Stack gap="lg">
          {criar.isError && (
            <MensagemErro
              erro={criar.error}
              titulo="Não foi possível criar a cotação"
              camposOcultos={[...CAMPOS_COTACAO, ...camposDasOpcoes(opcoes.length)]}
            />
          )}

          <Paper withBorder p="lg" component="section" aria-labelledby="cotacao-dados">
            <Title id="cotacao-dados" order={2} size="h4" mb="md">
              Dados da cotação
            </Title>
            <Stack gap="md">
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
                <Select
                  label="Produto"
                  description="Somente produtos ativos."
                  placeholder={produtos.isPending ? 'Carregando…' : 'Selecione'}
                  required
                  searchable
                  nothingFoundMessage="Nenhum produto encontrado"
                  data={(produtos.data ?? []).map((produto) => ({
                    value: String(produto.id),
                    label: produto.nome,
                  }))}
                  value={valores.produtoId || null}
                  error={
                    erros.produtoId ??
                    (produtos.isError ? 'Não foi possível carregar os produtos.' : undefined)
                  }
                  onChange={(valor) => {
                    alterar('produtoId', valor ?? '');
                  }}
                />
                <TextInput
                  label="Quantidade"
                  description="Quantidade da necessidade de compra."
                  required
                  inputMode="decimal"
                  value={valores.quantidade}
                  error={erros.quantidade}
                  onChange={(evento) => {
                    alterar('quantidade', evento.currentTarget.value);
                  }}
                />
              </SimpleGrid>
              <Textarea
                label="Descrição"
                description="Opcional."
                autosize
                minRows={2}
                value={valores.descricao}
                error={erros.descricao}
                onChange={(evento) => {
                  alterar('descricao', evento.currentTarget.value);
                }}
              />
            </Stack>
          </Paper>

          {opcoes.map((opcao, indice) => (
            <Paper
              key={opcao.chave}
              withBorder
              p="lg"
              component="section"
              aria-label={`Opção ${String(indice + 1)}`}
            >
              <Group justify="space-between" mb="md">
                <Title order={2} size="h4">
                  {`Opção ${String(indice + 1)}`}
                </Title>
                {opcoes.length > 1 && (
                  <Button
                    variant="subtle"
                    color="red"
                    size="xs"
                    onClick={() => {
                      removerOpcao(opcao.chave);
                    }}
                  >
                    {`Remover opção ${String(indice + 1)}`}
                  </Button>
                )}
              </Group>
              <CamposOpcao
                valores={opcao.valores}
                erros={opcao.erros}
                alterar={(campo, valor) => {
                  alterarOpcao(opcao.chave, campo, valor);
                }}
                produtoId={produtoId}
              />
            </Paper>
          ))}

          <Group justify="space-between">
            <Button
              variant="default"
              onClick={incluirOpcao}
              disabled={opcoes.length >= MAXIMO_OPCOES}
            >
              Adicionar outra opção
            </Button>
            <Group gap="sm">
              <Button component={Link} to={ROTAS.cotacoes} variant="default">
                Cancelar
              </Button>
              <Button type="submit" loading={criar.isPending}>
                Criar cotação e comparar
              </Button>
            </Group>
          </Group>
          <Text size="xs" c="dimmed">
            {`Máximo de ${String(MAXIMO_OPCOES)} opções por cotação. Os valores e dados fiscais informados só entram no cálculo quando os parâmetros de fonte estiverem configurados para usar os dados informados.`}
          </Text>
        </Stack>
      </form>
    </>
  );
}
