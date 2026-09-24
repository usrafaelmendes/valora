package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.Comparacao;

import java.util.Map;

/** Comparação com os cálculos de cada opção (por id do cálculo), para apresentar o detalhe. */
public record ComparacaoDetalhada(Comparacao comparacao, Map<Long, CalculoCusto> calculos) {
}
