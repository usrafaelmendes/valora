package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.validation.Cnpj;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados de cadastro e de atualização do fornecedor (RF03, CT05, CT06). */
public record FornecedorRequest(
        @NotBlank(message = "A razão social é obrigatória.")
        @Size(max = 150, message = "A razão social deve ter no máximo 150 caracteres.")
        String razaoSocial,

        @NotBlank(message = "O CNPJ é obrigatório.")
        @Size(max = 18, message = "O CNPJ informado é inválido.")
        @Cnpj
        String cnpj,

        @NotNull(message = "A UF de emissão é obrigatória.")
        Uf uf,

        @NotNull(message = "O tipo do fornecedor é obrigatório (FABRICANTE ou ATACADISTA).")
        TipoFornecedor tipo,

        @Size(max = 100, message = "O prazo de pagamento base deve ter no máximo 100 caracteres.")
        String prazoPagamentoBase) {
}
