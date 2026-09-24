package br.com.squadcore.comparaprecos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Operação a calcular. Informe nfeItemId (operação de uma NF-e importada) ou
 * fornecedorId + produtoId (operação informada, ex.: cotação).
 *
 * Quais dados são usados depende dos parâmetros FONTE_VALORES_OPERACAO e FONTE_DADOS_FISCAIS:
 * valores e dadosFiscais só são considerados quando a fonte configurada é INFORMADO.
 */
public record CalculoRequest(
        Long nfeItemId,
        Long fornecedorId,
        Long produtoId,

        @DecimalMin(value = "0", inclusive = false, message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 11, fraction = 4, message = "A quantidade aceita até 11 dígitos inteiros e 4 decimais.")
        BigDecimal quantidade,

        @Valid
        ValoresInformados valores,

        @Valid
        DadosFiscaisInformados dadosFiscais) {

    /** Componentes de valor informados; componente não informado fica indisponível (não é zero). */
    public record ValoresInformados(
            @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
            @Digits(integer = 13, fraction = 2, message = "Use no máximo 13 dígitos inteiros e 2 decimais.")
            BigDecimal valorProduto,

            @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
            @Digits(integer = 13, fraction = 2, message = "Use no máximo 13 dígitos inteiros e 2 decimais.")
            BigDecimal valorIpi,

            @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
            @Digits(integer = 13, fraction = 2, message = "Use no máximo 13 dígitos inteiros e 2 decimais.")
            BigDecimal valorFrete,

            @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
            @Digits(integer = 13, fraction = 2, message = "Use no máximo 13 dígitos inteiros e 2 decimais.")
            BigDecimal valorSeguro,

            @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
            @Digits(integer = 13, fraction = 2, message = "Use no máximo 13 dígitos inteiros e 2 decimais.")
            BigDecimal valorOutrasDespesas,

            @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
            @Digits(integer = 13, fraction = 2, message = "Use no máximo 13 dígitos inteiros e 2 decimais.")
            BigDecimal valorDesconto) {
    }

    /** Dados fiscais informados; alíquota não informada fica indisponível (não é zero). */
    public record DadosFiscaisInformados(
            @Pattern(regexp = "[0-8]", message = "A origem deve ser um código de 0 a 8 (tag orig da NF-e).")
            String origemMercadoria,

            @Pattern(regexp = "[0-9]{4}", message = "O CFOP deve ter 4 dígitos.")
            String cfop,

            @DecimalMin(value = "0", message = "A alíquota deve estar entre 0 e 100.")
            @DecimalMax(value = "100", message = "A alíquota deve estar entre 0 e 100.")
            @Digits(integer = 3, fraction = 4, message = "A alíquota aceita no máximo 4 casas decimais.")
            BigDecimal aliquotaIcms,

            @DecimalMin(value = "0", message = "A alíquota deve estar entre 0 e 100.")
            @DecimalMax(value = "100", message = "A alíquota deve estar entre 0 e 100.")
            @Digits(integer = 3, fraction = 4, message = "A alíquota aceita no máximo 4 casas decimais.")
            BigDecimal aliquotaIpi,

            @DecimalMin(value = "0", message = "A alíquota deve estar entre 0 e 100.")
            @DecimalMax(value = "100", message = "A alíquota deve estar entre 0 e 100.")
            @Digits(integer = 3, fraction = 4, message = "A alíquota aceita no máximo 4 casas decimais.")
            BigDecimal aliquotaPis,

            @DecimalMin(value = "0", message = "A alíquota deve estar entre 0 e 100.")
            @DecimalMax(value = "100", message = "A alíquota deve estar entre 0 e 100.")
            @Digits(integer = 3, fraction = 4, message = "A alíquota aceita no máximo 4 casas decimais.")
            BigDecimal aliquotaCofins) {
    }
}
