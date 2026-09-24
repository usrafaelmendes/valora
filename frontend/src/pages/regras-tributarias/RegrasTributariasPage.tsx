import { Badge, Button, Group, List, SegmentedControl, Select, Stack, Text } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { CHAVE_REGRAS_TRIBUTARIAS, regrasTributariasApi } from '../../api/regrasTributarias';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { ConfirmacaoAcao } from '../../components/ConfirmacaoAcao';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import { ROTAS } from '../../routes/rotas';
import {
  ROTULO_TRIBUTO,
  TRIBUTOS,
  type FiltroRegras,
  type RegraTributaria,
} from '../../types/regraTributaria';
import { formatarDataHora } from '../../utils/formatacao';
import { descreverCondicoes, descreverFator, descreverTaxa } from './apresentacaoRegra';
import { useCadastrosReferenciados } from './opcoesCadastros';
import { RegraFormulario } from './RegraFormulario';
import { ehTributo } from './validacaoRegra';

type FiltroSituacao = 'todas' | 'ativas' | 'inativas';

const OPCOES_SITUACAO = [
  { value: 'todas', label: 'Todas' },
  { value: 'ativas', label: 'Ativas' },
  { value: 'inativas', label: 'Inativas' },
];

const OPCOES_TRIBUTO = TRIBUTOS.map((tributo) => ({
  value: tributo,
  label: ROTULO_TRIBUTO[tributo],
}));

function paraFiltro(situacao: FiltroSituacao, tributo: string | null): FiltroRegras {
  return {
    ativa: situacao === 'todas' ? undefined : situacao === 'ativas',
    tributo: ehTributo(tributo) ? tributo : undefined,
  };
}

/** Formulário aberto: novo cadastro (regra null) ou edição. */
interface EstadoFormulario {
  regra: RegraTributaria | null;
}

/**
 * Regras tributárias configuráveis (REGRAS_TRIBUTARIAS §9). Área do ADMIN:
 * as regras são dados do banco, e nada aqui calcula créditos ou escolhe regras.
 */
export function RegrasTributariasPage() {
  const queryClient = useQueryClient();
  const cadastros = useCadastrosReferenciados();
  const [situacao, setSituacao] = useState<FiltroSituacao>('todas');
  const [tributo, setTributo] = useState<string | null>(null);
  const [formulario, setFormulario] = useState<EstadoFormulario | null>(null);
  const [emDesativacao, setEmDesativacao] = useState<RegraTributaria | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const filtro = paraFiltro(situacao, tributo);
  const consulta = useQuery({
    queryKey: [...CHAVE_REGRAS_TRIBUTARIAS, filtro],
    queryFn: ({ signal }) => regrasTributariasApi.listar(filtro, signal),
  });

  const desativar = useMutation({
    mutationFn: (regra: RegraTributaria) => regrasTributariasApi.desativar(regra.id),
    onSuccess: async (_resultado, regra) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_REGRAS_TRIBUTARIAS });
      setEmDesativacao(null);
      setAviso(`Regra "${regra.nome}" desativada. Ela continua cadastrada, com o histórico.`);
    },
    onError: () => {
      void queryClient.invalidateQueries({ queryKey: CHAVE_REGRAS_TRIBUTARIAS });
    },
  });

  const abrirCadastro = () => {
    setAviso(null);
    setFormulario({ regra: null });
  };

  const filtrando = situacao !== 'todas' || tributo !== null;

  const colunas: ColunaTabela<RegraTributaria>[] = [
    {
      titulo: 'Regra',
      conteudo: (regra) => (
        <Stack gap={2} maw={320}>
          <Text fw={500} size="sm">
            {regra.nome}
          </Text>
          {regra.observacao && (
            <Text size="xs" c="dimmed" lineClamp={3} title={regra.observacao}>
              {regra.observacao}
            </Text>
          )}
        </Stack>
      ),
    },
    {
      titulo: 'Tributo',
      semQuebra: true,
      conteudo: (regra) => ROTULO_TRIBUTO[regra.tributo],
    },
    { titulo: 'Origem da taxa', conteudo: (regra) => descreverTaxa(regra) },
    {
      titulo: 'Fator',
      alinhamento: 'right',
      conteudo: (regra) => descreverFator(regra.fator),
    },
    { titulo: 'Prioridade', alinhamento: 'right', conteudo: (regra) => regra.prioridade },
    {
      titulo: 'Condições',
      conteudo: (regra) => {
        const condicoes = descreverCondicoes(
          regra,
          cadastros.nomeFornecedor,
          cadastros.nomeProduto,
        );
        return condicoes.length === 0 ? (
          <Text size="sm" c="dimmed">
            Qualquer operação
          </Text>
        ) : (
          <List size="sm" spacing={2}>
            {condicoes.map((condicao) => (
              <List.Item key={condicao}>{condicao}</List.Item>
            ))}
          </List>
        );
      },
    },
    {
      titulo: 'Situação',
      conteudo: (regra) =>
        regra.ativa ? (
          <Badge color="teal" variant="light">
            Ativa
          </Badge>
        ) : (
          <Badge color="gray" variant="light">
            Inativa
          </Badge>
        ),
    },
    {
      titulo: 'Versão',
      semQuebra: true,
      conteudo: (regra) => (
        <Stack gap={0}>
          <Text size="sm">{`v${String(regra.versao)}`}</Text>
          <Text size="xs" c="dimmed">
            {formatarDataHora(regra.atualizadoEm)}
          </Text>
        </Stack>
      ),
    },
    {
      titulo: 'Ações',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (regra) => (
        <Group gap="xs" justify="flex-end" wrap="nowrap">
          <Button
            size="xs"
            variant="default"
            aria-label={`Editar ${regra.nome}`}
            onClick={() => {
              setAviso(null);
              setFormulario({ regra });
            }}
          >
            Editar
          </Button>
          {regra.ativa && (
            <Button
              size="xs"
              variant="subtle"
              color="red"
              aria-label={`Desativar ${regra.nome}`}
              onClick={() => {
                setAviso(null);
                desativar.reset();
                setEmDesativacao(regra);
              }}
            >
              Desativar
            </Button>
          )}
        </Group>
      ),
    },
  ];

  return (
    <>
      <CabecalhoPagina
        titulo="Regras tributárias"
        descricao="Como obter a alíquota de crédito de cada tributo e em quais operações ela vale. Os cálculos usam as regras ativas configuradas aqui."
        acoes={
          <>
            <Button component={Link} to={ROTAS.regrasAplicaveis} variant="default">
              Conferir regras aplicáveis
            </Button>
            <Button onClick={abrirCadastro}>Cadastrar regra</Button>
          </>
        }
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

        <Group gap="md" align="flex-end" wrap="wrap">
          <Stack gap={4}>
            <Text size="sm" fw={500} id="filtro-situacao">
              Situação
            </Text>
            <SegmentedControl
              aria-labelledby="filtro-situacao"
              data={OPCOES_SITUACAO}
              value={situacao}
              onChange={(valor) => {
                setSituacao(valor as FiltroSituacao);
              }}
            />
          </Stack>
          <Select
            label="Filtrar por tributo"
            placeholder="Todos"
            clearable
            data={OPCOES_TRIBUTO}
            value={tributo}
            onChange={setTributo}
            w={220}
          />
        </Group>

        <ResultadoConsulta
          consulta={consulta}
          mensagemCarregando="Carregando regras tributárias…"
          tituloErro="Não foi possível carregar as regras tributárias"
          vazio={
            filtrando ? (
              <EstadoVazio
                titulo="Nenhuma regra encontrada"
                descricao="Nenhuma regra corresponde aos filtros selecionados."
              />
            ) : (
              <EstadoVazio
                titulo="Nenhuma regra tributária cadastrada"
                descricao="Sem regras, os cálculos informam que a situação precisa ser configurada."
                acao={<Button onClick={abrirCadastro}>Cadastrar regra</Button>}
              />
            )
          }
        >
          {(regras) => (
            <TabelaDados
              rotulo="Regras tributárias"
              itens={regras}
              colunas={colunas}
              obterChave={(regra) => regra.id}
              larguraMinima={1100}
            />
          )}
        </ResultadoConsulta>
      </Stack>

      {formulario && (
        <RegraFormulario
          regra={formulario.regra}
          aoCancelar={() => {
            setFormulario(null);
          }}
          aoSalvar={(salva) => {
            setAviso(
              formulario.regra
                ? `Regra "${salva.nome}" atualizada (versão ${String(salva.versao)}).`
                : `Regra "${salva.nome}" cadastrada.`,
            );
            setFormulario(null);
          }}
        />
      )}

      {emDesativacao && (
        <ConfirmacaoAcao
          titulo="Desativar regra tributária"
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
            Deseja desativar a regra <strong>{emDesativacao.nome}</strong>?
          </Text>
          <Text size="sm" c="dimmed">
            Ela deixa de ser considerada nos próximos cálculos, mas não é apagada: continua na
            listagem como inativa e pode ser reativada pela edição. Cálculos e comparações já
            gravados mantêm o registro da regra e da versão usadas.
          </Text>
        </ConfirmacaoAcao>
      )}
    </>
  );
}
