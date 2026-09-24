package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.ComponenteValor;

import java.util.List;

/**
 * Parâmetros do cálculo vigentes no momento da execução. Campos nulos = não definidos.
 *
 * @param composicaoValorOperacao componentes do valor da operação
 * @param baseIgualValorOperacao  true quando a base dos créditos é o próprio valor da operação
 * @param composicaoBaseCreditos  componentes da base quando não é o valor da operação
 * @param arredondamento          POR_CREDITO ou SOMENTE_TOTAL
 * @param criterioArredondamento  MEIO_PARA_CIMA ou MEIO_PARA_PAR
 */
public record ConfiguracaoCalculo(List<ComponenteValor> composicaoValorOperacao,
                                  boolean baseIgualValorOperacao,
                                  List<ComponenteValor> composicaoBaseCreditos,
                                  String arredondamento,
                                  String criterioArredondamento) {
}
