package br.com.squadcore.comparaprecos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Nova cotação (RF08, CT23): produto, quantidade, condições e as opções de fornecedores. */
public record CotacaoRequest(
        @NotNull(message = "O produto é obrigatório.")
        Long produtoId,

        @NotNull(message = "A quantidade é obrigatória.")
        @DecimalMin(value = "0", inclusive = false, message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 11, fraction = 4, message = "A quantidade aceita até 11 dígitos inteiros e 4 decimais.")
        BigDecimal quantidade,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @NotEmpty(message = "Informe pelo menos uma opção de fornecedor.")
        @Size(max = CotacaoRequest.MAXIMO_OPCOES, message = "Informe no máximo " + CotacaoRequest.MAXIMO_OPCOES + " opções.")
        List<@NotNull(message = "A opção não pode ser nula.") @Valid OpcaoCotacaoRequest> opcoes) {

    public static final int MAXIMO_OPCOES = 50;
}
