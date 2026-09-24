package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;

import java.math.BigDecimal;
import java.time.Instant;

public record CalculoResumoResponse(Long id, StatusCalculo status, Instant executadoEm, Long nfeItemId,
                                    Long fornecedorId, Long produtoId, BigDecimal valorOperacao,
                                    BigDecimal totalCreditos, BigDecimal custoEfetivo) {

    public static CalculoResumoResponse de(CalculoCusto c) {
        return new CalculoResumoResponse(c.getId(), c.getStatus(), c.getExecutadoEm(), c.getNfeItemId(),
                c.getFornecedorId(), c.getProdutoId(), c.getValorOperacao(), c.getTotalCreditos(),
                c.getCustoEfetivo());
    }
}
