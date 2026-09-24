import { MultiSelect, Select, TagsInput, Text, TextInput } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CHAVE_PARAMETROS_CALCULO, parametrosCalculoApi } from '../../api/parametrosCalculo';
import { FormularioModal } from '../../components/FormularioModal';
import { useFormulario, type ErrosFormulario } from '../../hooks/useFormulario';
import type { ParametroCalculo } from '../../types/parametroCalculo';
import {
  formatoDoParametro,
  rotuloDoValor,
  tituloDoParametro,
  type FormatoParametro,
} from './apresentacaoParametro';

interface ValoresParametro extends Record<string, string> {
  valor: string;
}

function partes(valor: string): string[] {
  return valor
    .split(',')
    .map((parte) => parte.trim())
    .filter(Boolean);
}

/** Espelha o limite de ParametroCalculoRequest; o formato do valor é validado pelo backend. */
function validarParametro(valores: ValoresParametro): ErrosFormulario<ValoresParametro> {
  return {
    valor:
      valores.valor.trim().length > 500 ? 'O valor deve ter no máximo 500 caracteres.' : undefined,
  };
}

interface ParametroFormularioProps {
  parametro: ParametroCalculo;
  aoSalvar: (parametro: ParametroCalculo) => void;
  aoCancelar: () => void;
}

/**
 * Alteração do valor de um parâmetro (somente ADMIN). Sem valor, o parâmetro volta a
 * "não definido": o frontend nunca escolhe um valor por conta própria.
 */
export function ParametroFormulario({ parametro, aoSalvar, aoCancelar }: ParametroFormularioProps) {
  const queryClient = useQueryClient();
  const formato = formatoDoParametro(parametro.chave, parametro.valoresAceitos);
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    { valor: parametro.valor ?? '' },
    validarParametro,
  );

  const salvar = useMutation({
    mutationFn: (valor: string | null) =>
      parametrosCalculoApi.atualizar(parametro.chave, { valor }),
    onSuccess: async (salvo) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_PARAMETROS_CALCULO });
      aoSalvar(salvo);
    },
    onError: aplicarErrosApi,
  });

  const enviar = () => {
    if (validarTudo()) {
      salvar.mutate(valores.valor.trim() || null);
    }
  };

  return (
    <FormularioModal
      titulo={`Alterar: ${tituloDoParametro(parametro.chave)}`}
      rotuloSalvar="Salvar"
      salvando={salvar.isPending}
      erro={salvar.error}
      campos={['valor']}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      <Text size="sm">{parametro.descricao}</Text>
      <Text size="xs" c="dimmed">
        {`Chave: ${parametro.chave}. Deixe sem valor para voltar a "não definido". A alteração vale para os próximos cálculos; os já gravados mantêm a configuração que usaram.`}
      </Text>
      <CampoValor
        formato={formato}
        valoresAceitos={parametro.valoresAceitos}
        valor={valores.valor}
        erro={erros.valor}
        aoAlterar={(valor) => {
          alterar('valor', valor);
        }}
      />
    </FormularioModal>
  );
}

interface CampoValorProps {
  formato: FormatoParametro;
  valoresAceitos: string[];
  valor: string;
  erro?: string;
  aoAlterar: (valor: string) => void;
}

const NAO_DEFINIDO = 'Não definido';

/**
 * O Mantine oculta o botão de limpar de leitores de tela e do teclado; aqui ele é a forma de
 * voltar o parâmetro para "não definido", então fica acessível.
 */
const BOTAO_LIMPAR = {
  'aria-label': 'Limpar valor (não definido)',
  'aria-hidden': false,
  tabIndex: 0,
};

function CampoValor({ formato, valoresAceitos, valor, erro, aoAlterar }: CampoValorProps) {
  const opcoes = valoresAceitos.map((aceito) => ({
    value: aceito,
    label: rotuloDoValor(aceito) === aceito ? aceito : `${rotuloDoValor(aceito)} (${aceito})`,
  }));

  switch (formato) {
    case 'VALOR_UNICO':
      return (
        <Select
          label="Valor"
          placeholder={NAO_DEFINIDO}
          clearable
          clearButtonProps={BOTAO_LIMPAR}
          searchable={opcoes.length > 8}
          data-autofocus
          data={opcoes}
          value={valor || null}
          error={erro}
          onChange={(selecionado) => {
            aoAlterar(selecionado ?? '');
          }}
        />
      );
    case 'LISTA_VALORES':
      return (
        <MultiSelect
          label="Valores"
          description="Selecione um ou mais valores. O backend confere as combinações aceitas."
          placeholder={valor ? undefined : NAO_DEFINIDO}
          clearable
          clearButtonProps={BOTAO_LIMPAR}
          data-autofocus
          data={opcoes}
          value={partes(valor)}
          error={erro}
          onChange={(selecionados) => {
            aoAlterar(selecionados.join(','));
          }}
        />
      );
    case 'LISTA_CFOP':
      return (
        <TagsInput
          label="CFOPs"
          description="Códigos de 4 dígitos. Tecle Enter ou vírgula após cada um."
          placeholder={valor ? undefined : NAO_DEFINIDO}
          clearable
          clearButtonProps={BOTAO_LIMPAR}
          data-autofocus
          splitChars={[',', ' ', ';']}
          value={partes(valor)}
          error={erro}
          onChange={(cfops) => {
            aoAlterar(cfops.join(','));
          }}
        />
      );
    case 'TEXTO':
      return (
        <TextInput
          label="Valor"
          placeholder={NAO_DEFINIDO}
          description={
            valoresAceitos.length > 0 ? `Valores aceitos: ${valoresAceitos.join(', ')}.` : undefined
          }
          data-autofocus
          value={valor}
          error={erro}
          onChange={(evento) => {
            aoAlterar(evento.currentTarget.value);
          }}
        />
      );
  }
}
