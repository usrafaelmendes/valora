package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Tributo;

import java.util.List;

/**
 * Resultado da seleção de regras para um tributo.
 *
 * @param regras APLICAVEL: a regra escolhida; CONFLITO: as regras empatadas; SEM_REGRA: vazia
 */
public record SelecaoTributo(Tributo tributo, Situacao situacao, List<RegraTributaria> regras) {

    public enum Situacao {
        /** Uma única regra ativa de maior prioridade se aplica. */
        APLICAVEL,
        /** Nenhuma regra ativa se aplica: a situação precisa de configuração (CT17). */
        SEM_REGRA,
        /** Mais de uma regra ativa com a mesma maior prioridade: nenhuma é escolhida. */
        CONFLITO
    }

    public RegraTributaria regra() {
        return situacao == Situacao.APLICAVEL ? regras.getFirst() : null;
    }
}
