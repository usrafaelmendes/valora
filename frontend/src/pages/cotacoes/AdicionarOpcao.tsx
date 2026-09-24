import { useMutation, useQueryClient } from '@tanstack/react-query';
import { chaveCotacao, cotacoesApi } from '../../api/cotacoes';
import { FormularioModal } from '../../components/FormularioModal';
import { useFormulario } from '../../hooks/useFormulario';
import type { Cotacao, OpcaoCotacao, OpcaoCotacaoRequest } from '../../types/cotacao';
import { CamposOpcao } from './CamposOpcao';
import {
  CAMPOS_OPCAO,
  paraOpcaoRequest,
  validarOpcao,
  valoresIniciaisOpcao,
} from './validacaoCotacao';

interface AdicionarOpcaoProps {
  cotacao: Cotacao;
  aoAdicionar: (opcao: OpcaoCotacao) => void;
  aoCancelar: () => void;
}

/** Inclusão de uma opção em cotação existente (POST /cotacoes/{id}/opcoes). */
export function AdicionarOpcao({ cotacao, aoAdicionar, aoCancelar }: AdicionarOpcaoProps) {
  const queryClient = useQueryClient();
  const { valores, erros, alterar, validarTudo, aplicarErrosApi } = useFormulario(
    valoresIniciaisOpcao(),
    validarOpcao,
  );

  const adicionar = useMutation({
    mutationFn: (dados: OpcaoCotacaoRequest) => cotacoesApi.adicionarOpcao(cotacao.id, dados),
    onSuccess: async (opcao) => {
      // A cotação recarregada informa se há opção fora da última comparação.
      await queryClient.invalidateQueries({ queryKey: chaveCotacao(cotacao.id) });
      aoAdicionar(opcao);
    },
    onError: aplicarErrosApi,
  });

  const enviar = () => {
    if (validarTudo()) {
      adicionar.mutate(paraOpcaoRequest(valores));
    }
  };

  return (
    <FormularioModal
      titulo={`Adicionar opção à cotação nº ${String(cotacao.id)}`}
      rotuloSalvar="Adicionar opção"
      salvando={adicionar.isPending}
      erro={adicionar.error}
      campos={CAMPOS_OPCAO}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      <CamposOpcao
        valores={valores}
        erros={erros}
        alterar={alterar}
        produtoId={cotacao.produto.id}
      />
    </FormularioModal>
  );
}
