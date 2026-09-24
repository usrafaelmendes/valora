package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;

import java.math.BigDecimal;

/**
 * Opção de uma cotação pronta para ser classificada.
 *
 * @param produtoAtivo      situação do produto da cotação no momento da comparação
 * @param quantidadeCotacao quantidade da necessidade de compra
 * @param calculo           cálculo executado; nulo quando a opção não pôde ser calculada
 */
public record CandidatoComparacao(CotacaoOpcao opcao, boolean produtoAtivo, BigDecimal quantidadeCotacao,
                                  CalculoCusto calculo) {
}
