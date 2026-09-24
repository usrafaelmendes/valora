package br.com.squadcore.comparaprecos.calculation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado do cálculo. custoEfetivo só é preenchido quando não há pendência bloqueante.
 *
 * @param diferencaArredondamento totalCreditos − totalCreditosSemArredondamento
 */
public record ResultadoCalculo(BigDecimal valorOperacao, BigDecimal baseCreditos, List<CreditoCalculado> creditos,
                               BigDecimal totalCreditosSemArredondamento, BigDecimal totalCreditos,
                               BigDecimal diferencaArredondamento, BigDecimal custoEfetivo,
                               List<Pendencia> pendencias) {

    public boolean completo() {
        return custoEfetivo != null;
    }
}
