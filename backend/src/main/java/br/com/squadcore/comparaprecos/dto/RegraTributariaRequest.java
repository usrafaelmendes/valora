package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Dados de cadastro e de atualização de uma regra tributária (ADMIN).
 * As regras que dependem de mais de um campo são verificadas no RegraTributariaService.
 */
public record RegraTributariaRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres.")
        String observacao,

        @NotNull(message = "O tributo é obrigatório.")
        Tributo tributo,

        @NotNull(message = "A forma de obtenção da alíquota é obrigatória.")
        FormaAliquota formaAliquota,

        @DecimalMin(value = "0", message = "A alíquota deve estar entre 0 e 100.")
        @DecimalMax(value = "100", message = "A alíquota deve estar entre 0 e 100.")
        @Digits(integer = 3, fraction = 4, message = "A alíquota aceita no máximo 4 casas decimais.")
        BigDecimal aliquota,

        @DecimalMin(value = "0", inclusive = false, message = "O fator deve ser maior que zero.")
        @Digits(integer = 1, fraction = 4, message = "O fator deve ser menor que 10 e ter no máximo 4 casas decimais.")
        BigDecimal fator,

        @Min(value = 0, message = "A prioridade deve estar entre 0 e 1000.")
        @Max(value = 1000, message = "A prioridade deve estar entre 0 e 1000.")
        Integer prioridade,

        /** Opcional: nova regra começa ativa quando não informado. */
        Boolean ativa,

        TipoFornecedor tipoFornecedor,
        Long fornecedorId,
        Long produtoId,
        Uf ufOrigem,
        Uf ufDestino,
        AbrangenciaUf abrangenciaUf,

        @Size(max = 9, message = "Informe no máximo 9 origens.")
        Set<@NotNull(message = "Origem inválida.")
            @Pattern(regexp = "[0-8]", message = "Cada origem deve ser um código de 0 a 8 (tag orig da NF-e).")
            String> origensMercadoria,

        @Size(max = 100, message = "Informe no máximo 100 CFOPs.")
        Set<@NotNull(message = "CFOP inválido.")
            @Pattern(regexp = "[0-9]{4}", message = "Cada CFOP deve ter 4 dígitos.")
            String> cfops) {
}
