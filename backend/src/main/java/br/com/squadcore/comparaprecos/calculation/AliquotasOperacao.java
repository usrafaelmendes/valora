package br.com.squadcore.comparaprecos.calculation;

import java.math.BigDecimal;

/**
 * Alíquotas destacadas nos dados fiscais da operação (item da NF-e ou dados informados),
 * usadas pelas regras com forma ALIQUOTA_DA_NFE. Nulas quando ausentes.
 */
public record AliquotasOperacao(BigDecimal icms, BigDecimal ipi, BigDecimal pis, BigDecimal cofins) {

    public static final AliquotasOperacao NENHUMA = new AliquotasOperacao(null, null, null, null);
}
