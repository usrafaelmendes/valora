package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.ComponenteValor;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Tudo o que o cálculo usa, já obtido das fontes configuradas.
 *
 * @param componentes      valores disponíveis por componente (componente ausente = indisponível)
 * @param regrasAtivas     regras ativas no momento do cálculo
 * @param pendenciasPrevias pendências já encontradas ao obter os dados (parâmetros, fontes)
 */
public record EntradaCalculo(OperacaoTributavel operacao, Map<ComponenteValor, BigDecimal> componentes,
                             AliquotasOperacao aliquotas, ConfiguracaoCalculo configuracao,
                             List<RegraTributaria> regrasAtivas, List<Pendencia> pendenciasPrevias) {
}
