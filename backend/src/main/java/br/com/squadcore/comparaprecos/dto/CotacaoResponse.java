package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Cotacao;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.service.CotacaoService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cotação com as opções e a comparação mais recente.
 *
 * @param todasAsOpcoesComparadas false quando há opção incluída depois da última comparação
 *                                (é preciso executar uma nova comparação para incluí-la)
 */
public record CotacaoResponse(Long id, Produto produto, BigDecimal quantidade, String descricao, Instant criadoEm,
                              Long criadoPorId, List<OpcaoCotacaoResponse> opcoes, boolean todasAsOpcoesComparadas,
                              ComparacaoResponse ultimaComparacao) {

    public static CotacaoResponse de(CotacaoService.CotacaoDetalhada detalhe) {
        Cotacao c = detalhe.cotacao();
        Set<Long> comparadas = detalhe.ultimaComparacao() == null ? Set.of()
                : detalhe.ultimaComparacao().comparacao().getResultados().stream()
                        .map(ResultadoComparacao::getOpcaoId)
                        .collect(Collectors.toSet());
        boolean todas = c.getOpcoes().stream().map(CotacaoOpcao::getId).allMatch(comparadas::contains);
        return new CotacaoResponse(c.getId(),
                new Produto(c.getProduto().getId(), c.getProduto().getNome(), c.getProduto().isAtivo()),
                c.getQuantidade(), c.getDescricao(), c.getCriadoEm(), c.getCriadoPorId(),
                c.getOpcoes().stream().map(OpcaoCotacaoResponse::de).toList(), todas,
                detalhe.ultimaComparacao() == null ? null : ComparacaoResponse.de(detalhe.ultimaComparacao()));
    }

    public record Produto(Long id, String nome, boolean ativo) {
    }
}
