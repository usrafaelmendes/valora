package br.com.squadcore.comparaprecos.dto;

import jakarta.validation.constraints.Size;

/** Novo valor do parâmetro; nulo ou em branco volta a "não definido". */
public record ParametroCalculoRequest(
        @Size(max = 500, message = "O valor deve ter no máximo 500 caracteres.")
        String valor) {
}
