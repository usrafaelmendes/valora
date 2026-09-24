package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Cotacao;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoResumoResponse(Long id, Long produtoId, String produtoNome, BigDecimal quantidade,
                                    String descricao, Instant criadoEm, Long criadoPorId) {

    public static CotacaoResumoResponse de(Cotacao c) {
        return new CotacaoResumoResponse(c.getId(), c.getProduto().getId(), c.getProduto().getNome(),
                c.getQuantidade(), c.getDescricao(), c.getCriadoEm(), c.getCriadoPorId());
    }
}
