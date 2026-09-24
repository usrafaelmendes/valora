import { Select, SimpleGrid, TextInput } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CHAVE_FORNECEDORES, fornecedoresApi } from '../../api/fornecedores';
import { FormularioModal } from '../../components/FormularioModal';
import { useFormulario } from '../../hooks/useFormulario';
import {
  ROTULO_TIPO_FORNECEDOR,
  TIPOS_FORNECEDOR,
  UFS,
  type Fornecedor,
  type FornecedorRequest,
} from '../../types/fornecedor';
import {
  CAMPOS_FORNECEDOR,
  ehTipoFornecedor,
  ehUf,
  paraFornecedorRequest,
  validarFornecedor,
  valoresIniciaisFornecedor,
} from './validacaoFornecedor';

const OPCOES_UF = [...UFS].sort();

const OPCOES_TIPO = TIPOS_FORNECEDOR.map((tipo) => ({
  value: tipo,
  label: ROTULO_TIPO_FORNECEDOR[tipo],
}));

interface FornecedorFormularioProps {
  /** Fornecedor em edição; null para um novo cadastro. */
  fornecedor: Fornecedor | null;
  aoSalvar: (fornecedor: Fornecedor) => void;
  aoCancelar: () => void;
}

/** Cadastro e edição de fornecedor (somente ADMIN). */
export function FornecedorFormulario({
  fornecedor,
  aoSalvar,
  aoCancelar,
}: FornecedorFormularioProps) {
  const queryClient = useQueryClient();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    valoresIniciaisFornecedor(fornecedor),
    validarFornecedor,
  );

  const salvar = useMutation({
    mutationFn: (dados: FornecedorRequest) =>
      fornecedor ? fornecedoresApi.atualizar(fornecedor.id, dados) : fornecedoresApi.criar(dados),
    onSuccess: async (salvo) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_FORNECEDORES });
      aoSalvar(salvo);
    },
    onError: (erro) => {
      aplicarErrosApi(erro);
      // Ex.: 404 porque outro administrador desativou o fornecedor; a listagem é atualizada.
      void queryClient.invalidateQueries({ queryKey: CHAVE_FORNECEDORES });
    },
  });

  const enviar = () => {
    if (validarTudo()) {
      salvar.mutate(paraFornecedorRequest(valores));
    }
  };

  return (
    <FormularioModal
      titulo={fornecedor ? 'Editar fornecedor' : 'Cadastrar fornecedor'}
      rotuloSalvar={fornecedor ? 'Salvar alterações' : 'Cadastrar'}
      salvando={salvar.isPending}
      erro={salvar.error}
      campos={CAMPOS_FORNECEDOR}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      <TextInput
        label="Razão social"
        required
        data-autofocus
        value={valores.razaoSocial}
        error={erros.razaoSocial}
        onChange={(evento) => {
          alterar('razaoSocial', evento.currentTarget.value);
        }}
      />
      <TextInput
        label="CNPJ"
        description="Com ou sem máscara. Aceita o CNPJ numérico e o alfanumérico."
        required
        autoComplete="off"
        value={valores.cnpj}
        error={erros.cnpj}
        onChange={(evento) => {
          alterar('cnpj', evento.currentTarget.value);
        }}
      />
      <SimpleGrid cols={{ base: 1, xs: 2 }} spacing="md">
        <Select
          label="UF de emissão da NF-e"
          placeholder="Selecione"
          required
          searchable
          data={OPCOES_UF}
          value={valores.uf || null}
          error={erros.uf}
          onChange={(valor) => {
            alterar('uf', ehUf(valor) ? valor : '');
          }}
        />
        <Select
          label="Tipo do fornecedor"
          placeholder="Selecione"
          required
          data={OPCOES_TIPO}
          value={valores.tipo || null}
          error={erros.tipo}
          onChange={(valor) => {
            alterar('tipo', ehTipoFornecedor(valor) ? valor : '');
          }}
        />
      </SimpleGrid>
      <TextInput
        label="Prazo de pagamento base"
        description="Opcional. Texto livre, por exemplo: 28/56/84 dias."
        value={valores.prazoPagamentoBase}
        error={erros.prazoPagamentoBase}
        onChange={(evento) => {
          alterar('prazoPagamentoBase', evento.currentTarget.value);
        }}
      />
    </FormularioModal>
  );
}
