package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.validation.Gtin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de cadastro e de atualização do produto (RF04, CT07, CT08). */
public record ProdutoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 20, message = "O GTIN/EAN informado é inválido (use 8, 12, 13 ou 14 dígitos).")
        @Gtin
        String gtin) {
}
