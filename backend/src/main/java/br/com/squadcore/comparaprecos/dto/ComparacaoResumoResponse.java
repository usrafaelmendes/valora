package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Comparacao;

import java.time.Instant;

public record ComparacaoResumoResponse(Long id, Long cotacaoId, Instant executadoEm, Long executadoPorId,
                                       int totalOpcoes, int totalClassificadas) {

    public static ComparacaoResumoResponse de(Comparacao c) {
        return new ComparacaoResumoResponse(c.getId(), c.getCotacaoId(), c.getExecutadoEm(), c.getExecutadoPorId(),
                c.getTotalOpcoes(), c.getTotalClassificadas());
    }
}
