import {
  Alert,
  Divider,
  MultiSelect,
  Select,
  SimpleGrid,
  Switch,
  TagsInput,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CHAVE_REGRAS_TRIBUTARIAS, regrasTributariasApi } from '../../api/regrasTributarias';
import { FormularioModal } from '../../components/FormularioModal';
import { useFormulario } from '../../hooks/useFormulario';
import {
  ABRANGENCIAS_UF,
  DESCRICAO_FORMA_ALIQUOTA,
  FORMAS_ALIQUOTA,
  ROTULO_ABRANGENCIA_UF,
  ROTULO_FORMA_ALIQUOTA,
  ROTULO_TRIBUTO,
  TRIBUTOS,
  type RegraTributaria,
  type RegraTributariaRequest,
} from '../../types/regraTributaria';
import { ehTipoFornecedor, ehUf } from '../fornecedores/validacaoFornecedor';
import {
  OPCOES_ORIGEM_MERCADORIA,
  OPCOES_TIPO_FORNECEDOR,
  OPCOES_UF,
  useCadastrosReferenciados,
} from './opcoesCadastros';
import {
  CAMPOS_REGRA,
  deLista,
  ehAbrangenciaUf,
  ehFormaAliquota,
  ehTributo,
  paraLista,
  paraRegraRequest,
  usaAliquota,
  usaFator,
  validarRegra,
  valoresIniciaisRegra,
} from './validacaoRegra';

const OPCOES_TRIBUTO = TRIBUTOS.map((tributo) => ({
  value: tributo,
  label: ROTULO_TRIBUTO[tributo],
}));

const OPCOES_FORMA = FORMAS_ALIQUOTA.map((forma) => ({
  value: forma,
  label: ROTULO_FORMA_ALIQUOTA[forma],
}));

const OPCOES_ABRANGENCIA = ABRANGENCIAS_UF.map((abrangencia) => ({
  value: abrangencia,
  label: ROTULO_ABRANGENCIA_UF[abrangencia],
}));

const QUALQUER = 'Qualquer';

interface RegraFormularioProps {
  /** Regra em edição; null para um novo cadastro. */
  regra: RegraTributaria | null;
  aoSalvar: (regra: RegraTributaria) => void;
  aoCancelar: () => void;
}

/**
 * Cadastro e edição de regra tributária (somente ADMIN). O formulário só coleta a
 * configuração: nenhuma alíquota ou condição é sugerida, e quem decide qual regra se aplica
 * é o backend.
 */
export function RegraFormulario({ regra, aoSalvar, aoCancelar }: RegraFormularioProps) {
  const queryClient = useQueryClient();
  const cadastros = useCadastrosReferenciados();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    valoresIniciaisRegra(regra),
    validarRegra,
  );

  const salvar = useMutation({
    mutationFn: (dados: RegraTributariaRequest) =>
      regra ? regrasTributariasApi.atualizar(regra.id, dados) : regrasTributariasApi.criar(dados),
    onSuccess: async (salva) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_REGRAS_TRIBUTARIAS });
      aoSalvar(salva);
    },
    onError: (erro) => {
      aplicarErrosApi(erro);
      void queryClient.invalidateQueries({ queryKey: CHAVE_REGRAS_TRIBUTARIAS });
    },
  });

  const enviar = () => {
    if (validarTudo()) {
      salvar.mutate(paraRegraRequest(valores));
    }
  };

  const forma = valores.formaAliquota;

  return (
    <FormularioModal
      titulo={regra ? 'Editar regra tributária' : 'Cadastrar regra tributária'}
      rotuloSalvar={regra ? 'Salvar alterações' : 'Cadastrar'}
      salvando={salvar.isPending}
      erro={salvar.error}
      campos={CAMPOS_REGRA}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      {regra && (
        <Alert color="blue" variant="light">
          {`Ao salvar, a regra passa da versão ${String(regra.versao)} para uma nova versão. `}
          Cálculos e comparações já gravados mantêm a cópia da versão que usaram; a alteração vale
          somente para os próximos cálculos.
        </Alert>
      )}

      <TextInput
        label="Nome"
        description="Identifica a regra na listagem e no registro dos cálculos. Não pode repetir."
        required
        data-autofocus
        value={valores.nome}
        error={erros.nome}
        onChange={(evento) => {
          alterar('nome', evento.currentTarget.value);
        }}
      />
      <Textarea
        label="Observação"
        description="Opcional. Ex.: origem da regra ou justificativa da configuração."
        autosize
        minRows={2}
        maxRows={5}
        value={valores.observacao}
        error={erros.observacao}
        onChange={(evento) => {
          alterar('observacao', evento.currentTarget.value);
        }}
      />

      <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
        <Select
          label="Tributo"
          placeholder="Selecione"
          required
          data={OPCOES_TRIBUTO}
          value={valores.tributo || null}
          error={erros.tributo}
          onChange={(valor) => {
            alterar('tributo', ehTributo(valor) ? valor : '');
          }}
        />
        <Select
          label="Forma de obtenção da alíquota"
          placeholder="Selecione"
          required
          data={OPCOES_FORMA}
          value={forma || null}
          error={erros.formaAliquota}
          description={forma ? DESCRICAO_FORMA_ALIQUOTA[forma] : undefined}
          onChange={(valor) => {
            alterar('formaAliquota', ehFormaAliquota(valor) ? valor : '');
          }}
        />
      </SimpleGrid>

      <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="md">
        {usaAliquota(forma) && (
          <TextInput
            label="Alíquota (%)"
            description="De 0 a 100, até 4 casas."
            required
            inputMode="decimal"
            value={valores.aliquota}
            error={erros.aliquota}
            onChange={(evento) => {
              alterar('aliquota', evento.currentTarget.value);
            }}
          />
        )}
        {usaFator(forma) && (
          <TextInput
            label="Fator"
            description="Multiplica a alíquota obtida (ex.: 0,8 = 80%). Vazio = 1."
            inputMode="decimal"
            value={valores.fator}
            error={erros.fator}
            onChange={(evento) => {
              alterar('fator', evento.currentTarget.value);
            }}
          />
        )}
        <TextInput
          label="Prioridade"
          description="De 0 a 1000. Vazio = 0."
          inputMode="numeric"
          value={valores.prioridade}
          error={erros.prioridade}
          onChange={(evento) => {
            alterar('prioridade', evento.currentTarget.value);
          }}
        />
      </SimpleGrid>
      <Text size="xs" c="dimmed" mt={-8}>
        Entre regras ativas que se aplicam ao mesmo tributo, vale a de maior prioridade. Prioridades
        iguais geram conflito, e o sistema não escolhe nenhuma.
      </Text>

      <Switch
        label="Regra ativa"
        description="Somente regras ativas são consideradas nos cálculos."
        checked={valores.ativa === 'true'}
        error={erros.ativa}
        onChange={(evento) => {
          alterar('ativa', evento.currentTarget.checked ? 'true' : 'false');
        }}
      />

      <Divider
        label="Condições (vazias = qualquer valor)"
        labelPosition="left"
        mt="xs"
        styles={{ label: { fontSize: 'var(--mantine-font-size-sm)' } }}
      />

      <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
        <Select
          label="Tipo de fornecedor"
          placeholder={QUALQUER}
          clearable
          data={OPCOES_TIPO_FORNECEDOR}
          value={valores.tipoFornecedor || null}
          error={erros.tipoFornecedor}
          onChange={(valor) => {
            alterar('tipoFornecedor', ehTipoFornecedor(valor) ? valor : '');
          }}
        />
        <Select
          label="Fornecedor"
          placeholder={cadastros.fornecedores.isPending ? 'Carregando…' : QUALQUER}
          clearable
          searchable
          nothingFoundMessage="Nenhum fornecedor encontrado"
          data={cadastros.opcoesFornecedor(valores.fornecedorId)}
          value={valores.fornecedorId || null}
          error={
            erros.fornecedorId ??
            (cadastros.fornecedores.isError
              ? 'Não foi possível carregar os fornecedores.'
              : undefined)
          }
          onChange={(valor) => {
            alterar('fornecedorId', valor ?? '');
          }}
        />
        <Select
          label="Produto"
          placeholder={cadastros.produtos.isPending ? 'Carregando…' : QUALQUER}
          clearable
          searchable
          nothingFoundMessage="Nenhum produto encontrado"
          data={cadastros.opcoesProduto(valores.produtoId)}
          value={valores.produtoId || null}
          error={
            erros.produtoId ??
            (cadastros.produtos.isError ? 'Não foi possível carregar os produtos.' : undefined)
          }
          onChange={(valor) => {
            alterar('produtoId', valor ?? '');
          }}
        />
        <Select
          label="Operação (abrangência)"
          placeholder={QUALQUER}
          clearable
          data={OPCOES_ABRANGENCIA}
          value={valores.abrangenciaUf || null}
          error={erros.abrangenciaUf}
          onChange={(valor) => {
            alterar('abrangenciaUf', ehAbrangenciaUf(valor) ? valor : '');
          }}
        />
        <Select
          label="UF de origem"
          placeholder={QUALQUER}
          clearable
          searchable
          data={OPCOES_UF}
          value={valores.ufOrigem || null}
          error={erros.ufOrigem}
          onChange={(valor) => {
            alterar('ufOrigem', ehUf(valor) ? valor : '');
          }}
        />
        <Select
          label="UF de destino"
          placeholder={QUALQUER}
          clearable
          searchable
          data={OPCOES_UF}
          value={valores.ufDestino || null}
          error={erros.ufDestino}
          onChange={(valor) => {
            alterar('ufDestino', ehUf(valor) ? valor : '');
          }}
        />
      </SimpleGrid>

      <MultiSelect
        label="Origem da mercadoria (tag orig)"
        description="A regra vale quando a origem do item é uma das selecionadas."
        placeholder={valores.origensMercadoria ? undefined : QUALQUER}
        clearable
        data={OPCOES_ORIGEM_MERCADORIA}
        value={paraLista(valores.origensMercadoria)}
        error={erros.origensMercadoria}
        onChange={(selecionadas) => {
          alterar('origensMercadoria', deLista([...selecionadas].sort()));
        }}
      />
      <TagsInput
        label="CFOPs"
        description="Códigos de 4 dígitos. Tecle Enter ou vírgula após cada um."
        placeholder={valores.cfops ? undefined : QUALQUER}
        clearable
        splitChars={[',', ' ', ';']}
        value={paraLista(valores.cfops)}
        error={erros.cfops}
        onChange={(cfops) => {
          alterar('cfops', deLista(cfops.map((cfop) => cfop.trim()).filter(Boolean)));
        }}
      />
    </FormularioModal>
  );
}
