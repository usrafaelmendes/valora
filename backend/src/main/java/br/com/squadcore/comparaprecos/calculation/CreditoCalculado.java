package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Tributo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Crédito de um tributo. regra é a regra utilizada; em conflito, regrasEmConflito lista as empatadas.
 *
 * @param aliquotaObtida        alíquota da regra (PERCENTUAL_FIXO) ou da operação (ALIQUOTA_DA_NFE)
 * @param aliquotaAplicada      aliquotaObtida × fator
 * @param valorSemArredondamento base × aliquotaAplicada / 100, sem arredondamento
 * @param valor                 valor arredondado a 2 casas (nulo quando o arredondamento é só no total ou está pendente)
 */
public record CreditoCalculado(Tributo tributo, SituacaoCredito situacao, RegraTributaria regra,
                               List<RegraTributaria> regrasEmConflito, BigDecimal aliquotaObtida,
                               BigDecimal fator, BigDecimal aliquotaAplicada, BigDecimal base,
                               BigDecimal valorSemArredondamento, BigDecimal valor, String mensagem) {

    CreditoCalculado comValor(BigDecimal novoValor) {
        return new CreditoCalculado(tributo, situacao, regra, regrasEmConflito, aliquotaObtida, fator,
                aliquotaAplicada, base, valorSemArredondamento, novoValor, mensagem);
    }

    boolean calculado() {
        return valorSemArredondamento != null;
    }
}
