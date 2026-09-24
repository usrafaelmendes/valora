package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.calculation.OperacaoTributavel;
import br.com.squadcore.comparaprecos.calculation.SelecaoTributo;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Tributo;

import java.math.BigDecimal;
import java.util.List;

/** Regras selecionadas por tributo. Não contém créditos calculados: somente a seleção. */
public record RegrasAplicaveisResponse(OperacaoTributavel operacao, List<Selecao> tributos) {

    public static RegrasAplicaveisResponse de(OperacaoTributavel operacao, List<SelecaoTributo> selecoes) {
        return new RegrasAplicaveisResponse(operacao, selecoes.stream().map(Selecao::de).toList());
    }

    public record Selecao(Tributo tributo, SelecaoTributo.Situacao situacao, String mensagem, List<Regra> regras) {

        static Selecao de(SelecaoTributo selecao) {
            return new Selecao(selecao.tributo(), selecao.situacao(), mensagem(selecao.situacao()),
                    selecao.regras().stream().map(Regra::de).toList());
        }

        private static String mensagem(SelecaoTributo.Situacao situacao) {
            return switch (situacao) {
                case APLICAVEL -> null;
                case SEM_REGRA -> "Nenhuma regra ativa se aplica a esta operação. "
                        + "A situação precisa ser configurada ou validada.";
                case CONFLITO -> "Mais de uma regra ativa se aplica com a mesma prioridade. "
                        + "Ajuste as prioridades ou as condições das regras.";
            };
        }
    }

    public record Regra(Long id, String nome, int versao, Tributo tributo, FormaAliquota formaAliquota,
                        BigDecimal aliquota, BigDecimal fator, int prioridade) {

        static Regra de(RegraTributaria regra) {
            return new Regra(regra.getId(), regra.getNome(), regra.getVersao(), regra.getTributo(),
                    regra.getFormaAliquota(), regra.getAliquota(), regra.getFator(), regra.getPrioridade());
        }
    }
}
