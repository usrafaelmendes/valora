import { Textarea, TextInput } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CHAVE_PRODUTOS, produtosApi } from '../../api/produtos';
import { FormularioModal } from '../../components/FormularioModal';
import { useFormulario } from '../../hooks/useFormulario';
import type { Produto, ProdutoRequest } from '../../types/produto';
import {
  CAMPOS_PRODUTO,
  paraProdutoRequest,
  validarProduto,
  valoresIniciaisProduto,
} from './validacaoProduto';

interface ProdutoFormularioProps {
  /** Produto em edição; null para um novo cadastro. */
  produto: Produto | null;
  aoSalvar: (produto: Produto) => void;
  aoCancelar: () => void;
}

/** Cadastro e edição de produto (somente ADMIN). */
export function ProdutoFormulario({ produto, aoSalvar, aoCancelar }: ProdutoFormularioProps) {
  const queryClient = useQueryClient();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    valoresIniciaisProduto(produto),
    validarProduto,
  );

  const salvar = useMutation({
    mutationFn: (dados: ProdutoRequest) =>
      produto ? produtosApi.atualizar(produto.id, dados) : produtosApi.criar(dados),
    onSuccess: async (salvo) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_PRODUTOS });
      aoSalvar(salvo);
    },
    onError: (erro) => {
      aplicarErrosApi(erro);
      // Ex.: 404 porque outro administrador desativou o produto; a listagem é atualizada.
      void queryClient.invalidateQueries({ queryKey: CHAVE_PRODUTOS });
    },
  });

  const enviar = () => {
    if (validarTudo()) {
      salvar.mutate(paraProdutoRequest(valores));
    }
  };

  return (
    <FormularioModal
      titulo={produto ? 'Editar produto' : 'Cadastrar produto'}
      rotuloSalvar={produto ? 'Salvar alterações' : 'Cadastrar'}
      salvando={salvar.isPending}
      erro={salvar.error}
      campos={CAMPOS_PRODUTO}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      <TextInput
        label="Nome"
        description="Identifica o produto nas cotações. Não pode repetir o nome de outro produto."
        required
        data-autofocus
        value={valores.nome}
        error={erros.nome}
        onChange={(evento) => {
          alterar('nome', evento.currentTarget.value);
        }}
      />
      <Textarea
        label="Descrição"
        autosize
        minRows={2}
        maxRows={6}
        value={valores.descricao}
        error={erros.descricao}
        onChange={(evento) => {
          alterar('descricao', evento.currentTarget.value);
        }}
      />
      <TextInput
        label="GTIN/EAN"
        description="Opcional. Código de barras com 8, 12, 13 ou 14 dígitos (tag cEAN da NF-e)."
        inputMode="numeric"
        value={valores.gtin}
        error={erros.gtin}
        onChange={(evento) => {
          alterar('gtin', evento.currentTarget.value);
        }}
      />
    </FormularioModal>
  );
}
