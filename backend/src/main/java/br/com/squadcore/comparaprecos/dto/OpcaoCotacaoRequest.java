package br.com.squadcore.comparaprecos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Opção de fornecedor de uma cotação. Informe fornecedorId e/ou nfeItemId (item de NF-e de
 * referência; o fornecedor passa a ser o emitente da nota).
 *
 * valores e dadosFiscais seguem o mesmo formato do cálculo e só são usados quando os parâmetros
 * FONTE_VALORES_OPERACAO / FONTE_DADOS_FISCAIS forem INFORMADO. Os valores são os da operação
 * inteira, para a quantidade da cotação.
 */
public record OpcaoCotacaoRequest(
        Long fornecedorId,
        Long nfeItemId,

        @Size(max = 100, message = "A condição de pagamento deve ter no máximo 100 caracteres.")
        String condicaoPagamento,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observacao,

        @Valid
        CalculoRequest.ValoresInformados valores,

        @Valid
        CalculoRequest.DadosFiscaisInformados dadosFiscais) {
}
