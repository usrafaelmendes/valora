package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import jakarta.validation.constraints.Pattern;

/**
 * Operação de exemplo para o ADMIN conferir quais regras configuradas se aplicariam.
 * Campos ausentes são tratados como desconhecidos. Sem ufDestino, usa o parâmetro UF_DESTINO.
 */
public record RegrasAplicaveisRequest(
        Long fornecedorId,
        TipoFornecedor tipoFornecedor,
        Long produtoId,
        Uf ufOrigem,
        Uf ufDestino,

        @Pattern(regexp = "[0-8]", message = "A origem deve ser um código de 0 a 8 (tag orig da NF-e).")
        String origemMercadoria,

        @Pattern(regexp = "[0-9]{4}", message = "O CFOP deve ter 4 dígitos.")
        String cfop) {
}
