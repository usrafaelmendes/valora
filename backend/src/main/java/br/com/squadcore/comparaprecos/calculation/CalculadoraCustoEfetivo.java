package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.ComponenteValor;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Tributo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custo efetivo = valor da operação − soma dos créditos aplicáveis (REGRAS_TRIBUTARIAS §4).
 *
 * Cada crédito é calculado individualmente sobre a base, sem cascata:
 * crédito = base × (alíquota obtida × fator) / 100. As alíquotas, fatores e condições vêm
 * exclusivamente das regras configuradas; este código não conhece nenhum valor tributário.
 *
 * Nada é presumido: parâmetro não definido, regra ausente, conflito ou dado indisponível
 * geram pendência bloqueante, e o custo efetivo fica nulo (INCOMPLETO).
 */
@Component
public class CalculadoraCustoEfetivo {

    private static final int CASAS_MONETARIAS = 2;

    private final SeletorRegrasTributarias seletor;

    public CalculadoraCustoEfetivo(SeletorRegrasTributarias seletor) {
        this.seletor = seletor;
    }

    public ResultadoCalculo calcular(EntradaCalculo entrada) {
        List<Pendencia> pendencias = new ArrayList<>(entrada.pendenciasPrevias());
        ConfiguracaoCalculo config = entrada.configuracao();

        BigDecimal valorOperacao = null;
        if (config.composicaoValorOperacao() == null) {
            pendencias.add(new Pendencia(TipoPendencia.PARAMETRO_NAO_DEFINIDO,
                    "Parâmetro COMPOSICAO_VALOR_OPERACAO não definido: não é possível obter o valor da operação."));
        } else {
            valorOperacao = somar(config.composicaoValorOperacao(), entrada.componentes(), "valor da operação", pendencias);
        }

        BigDecimal base = null;
        if (config.baseIgualValorOperacao()) {
            base = valorOperacao;
        } else if (config.composicaoBaseCreditos() == null) {
            pendencias.add(new Pendencia(TipoPendencia.PARAMETRO_NAO_DEFINIDO,
                    "Parâmetro COMPOSICAO_BASE_CREDITOS não definido: não é possível obter a base dos créditos."));
        } else {
            base = somar(config.composicaoBaseCreditos(), entrada.componentes(), "base dos créditos", pendencias);
        }

        List<SelecaoTributo> selecoes = seletor.selecionar(entrada.operacao(), entrada.regrasAtivas());
        List<CreditoCalculado> creditos = creditos(selecoes, entrada.aliquotas(), base, pendencias);

        return totalizar(valorOperacao, base, creditos, config, pendencias);
    }

    // ---- Valor da operação e base ----

    private static BigDecimal somar(List<ComponenteValor> composicao, Map<ComponenteValor, BigDecimal> componentes,
                                    String descricao, List<Pendencia> pendencias) {
        BigDecimal total = BigDecimal.ZERO;
        boolean completo = true;
        for (ComponenteValor componente : composicao) {
            BigDecimal valor = componentes.get(componente);
            if (valor == null) {
                pendencias.add(new Pendencia(TipoPendencia.DADO_INDISPONIVEL,
                        "Componente " + componente + " do " + descricao + " não está disponível na fonte configurada."));
                completo = false;
            } else {
                total = componente.subtrai() ? total.subtract(valor) : total.add(valor);
            }
        }
        if (!completo) {
            return null;
        }
        if (total.signum() < 0) {
            pendencias.add(new Pendencia(TipoPendencia.VALOR_INVALIDO,
                    "O " + descricao + " calculado é negativo (" + total.toPlainString() + ")."));
            return null;
        }
        return total.setScale(CASAS_MONETARIAS, RoundingMode.UNNECESSARY);
    }

    // ---- Créditos ----

    private static List<CreditoCalculado> creditos(List<SelecaoTributo> selecoes, AliquotasOperacao aliquotas,
                                                   BigDecimal base, List<Pendencia> pendencias) {
        SelecaoTributo icms = selecao(selecoes, Tributo.ICMS);
        SelecaoTributo ipi = selecao(selecoes, Tributo.IPI);
        SelecaoTributo pis = selecao(selecoes, Tributo.PIS);
        SelecaoTributo cofins = selecao(selecoes, Tributo.COFINS);

        List<CreditoCalculado> creditos = new ArrayList<>();
        creditos.add(credito(icms, aliquotas, base, pendencias));
        creditos.add(credito(ipi, aliquotas, base, pendencias));

        boolean pisCombinado = usaRegraCombinada(pis);
        boolean cofinsCombinado = usaRegraCombinada(cofins);
        if (pisCombinado && cofinsCombinado && pis.regra() == cofins.regra()) {
            // A regra PIS_COFINS cobre os dois tributos: aplicada uma única vez.
            creditos.add(credito(pis, aliquotas, base, pendencias));
        } else if (pisCombinado || cofinsCombinado) {
            String mensagem = "A regra combinada PIS_COFINS foi escolhida para "
                    + (pisCombinado ? "PIS" : "COFINS") + ", mas não para "
                    + (pisCombinado ? "COFINS (" + cofins.situacao() + ")" : "PIS (" + pis.situacao() + ")")
                    + ". Aplicá-la levaria a crédito duplicado ou incompleto: ajuste as prioridades ou desative uma das regras.";
            pendencias.add(new Pendencia(TipoPendencia.CONFIGURACAO_INCONSISTENTE, mensagem));
            creditos.add(new CreditoCalculado(Tributo.PIS_COFINS, SituacaoCredito.CONFIGURACAO_INCONSISTENTE, null,
                    List.of(), null, null, null, base, null, null, mensagem));
        } else {
            creditos.add(credito(pis, aliquotas, base, pendencias));
            creditos.add(credito(cofins, aliquotas, base, pendencias));
        }
        return creditos;
    }

    private static CreditoCalculado credito(SelecaoTributo selecao, AliquotasOperacao aliquotas, BigDecimal base,
                                            List<Pendencia> pendencias) {
        Tributo tributo = selecao.tributo();
        switch (selecao.situacao()) {
            case SEM_REGRA -> {
                String mensagem = "Nenhuma regra ativa de " + tributo + " se aplica a esta operação. "
                        + "O crédito não foi presumido: configure ou valide a regra.";
                pendencias.add(new Pendencia(TipoPendencia.REGRA_AUSENTE, mensagem));
                return new CreditoCalculado(tributo, SituacaoCredito.REGRA_AUSENTE, null, List.of(),
                        null, null, null, base, null, null, mensagem);
            }
            case CONFLITO -> {
                String mensagem = "Mais de uma regra ativa de " + tributo + " se aplica com a mesma prioridade: "
                        + nomes(selecao.regras()) + ". Nenhuma foi escolhida.";
                pendencias.add(new Pendencia(TipoPendencia.CONFLITO_REGRAS, mensagem));
                return new CreditoCalculado(tributo, SituacaoCredito.CONFLITO_REGRAS, null, selecao.regras(),
                        null, null, null, base, null, null, mensagem);
            }
            default -> {
                return creditoDaRegra(selecao.regra(), aliquotas, base, pendencias);
            }
        }
    }

    private static CreditoCalculado creditoDaRegra(RegraTributaria regra, AliquotasOperacao aliquotas,
                                                   BigDecimal base, List<Pendencia> pendencias) {
        Tributo tributo = regra.getTributo();
        if (regra.getFormaAliquota() == FormaAliquota.SEM_CREDITO) {
            return new CreditoCalculado(tributo, SituacaoCredito.SEM_CREDITO, regra, List.of(), null, null,
                    BigDecimal.ZERO, base, base == null ? null : BigDecimal.ZERO,
                    base == null ? null : BigDecimal.ZERO.setScale(CASAS_MONETARIAS), null);
        }

        BigDecimal obtida = regra.getFormaAliquota() == FormaAliquota.PERCENTUAL_FIXO
                ? regra.getAliquota()
                : aliquotaDaOperacao(tributo, aliquotas);
        if (obtida == null) {
            String mensagem = "A regra \"" + regra.getNome() + "\" usa a alíquota de " + tributo
                    + " destacada nos dados fiscais da operação, mas ela não está disponível.";
            pendencias.add(new Pendencia(TipoPendencia.ALIQUOTA_INDISPONIVEL, mensagem));
            return new CreditoCalculado(tributo, SituacaoCredito.ALIQUOTA_INDISPONIVEL, regra, List.of(),
                    null, regra.getFator(), null, base, null, null, mensagem);
        }

        BigDecimal aplicada = obtida.multiply(regra.getFator());
        if (base == null) {
            return new CreditoCalculado(tributo, SituacaoCredito.NAO_CALCULADO, regra, List.of(), obtida,
                    regra.getFator(), aplicada, null, null, null, "Base dos créditos indisponível.");
        }
        // Sobre a base (nunca sobre o resultado de outro crédito): sem cascata.
        BigDecimal valor = base.multiply(aplicada).movePointLeft(2);
        return new CreditoCalculado(tributo, SituacaoCredito.CALCULADO, regra, List.of(), obtida,
                regra.getFator(), aplicada, base, valor, null, null);
    }

    /** PIS_COFINS com alíquota da operação = pPIS + pCOFINS (as duas precisam existir). */
    private static BigDecimal aliquotaDaOperacao(Tributo tributo, AliquotasOperacao aliquotas) {
        return switch (tributo) {
            case ICMS -> aliquotas.icms();
            case IPI -> aliquotas.ipi();
            case PIS -> aliquotas.pis();
            case COFINS -> aliquotas.cofins();
            case PIS_COFINS -> aliquotas.pis() == null || aliquotas.cofins() == null
                    ? null : aliquotas.pis().add(aliquotas.cofins());
        };
    }

    // ---- Totais e arredondamento ----

    private static ResultadoCalculo totalizar(BigDecimal valorOperacao, BigDecimal base,
                                              List<CreditoCalculado> creditos, ConfiguracaoCalculo config,
                                              List<Pendencia> pendencias) {
        boolean todosCalculados = creditos.stream().allMatch(CreditoCalculado::calculado);
        BigDecimal totalSemArredondamento = todosCalculados
                ? creditos.stream().map(CreditoCalculado::valorSemArredondamento).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;

        boolean precisaArredondar = creditos.stream().filter(CreditoCalculado::calculado)
                .anyMatch(credito -> precisaArredondar(credito.valorSemArredondamento()));
        RoundingMode modo = null;
        String momento = null;
        if (!precisaArredondar) {
            // Todos os créditos já têm no máximo 2 casas: o arredondamento não altera nada.
            creditos = creditos.stream().map(credito -> credito.calculado()
                    ? credito.comValor(credito.valorSemArredondamento().setScale(CASAS_MONETARIAS)) : credito).toList();
        } else {
            modo = modo(config.criterioArredondamento(), pendencias);
            momento = momento(config.arredondamento(), pendencias);
            if (modo != null && "POR_CREDITO".equals(momento)) {
                RoundingMode escolhido = modo;
                creditos = creditos.stream().map(credito -> credito.calculado()
                        ? credito.comValor(credito.valorSemArredondamento().setScale(CASAS_MONETARIAS, escolhido))
                        : credito).toList();
            }
        }

        BigDecimal totalCreditos = null;
        if (todosCalculados) {
            if (!precisaArredondar || "POR_CREDITO".equals(momento) && modo != null) {
                totalCreditos = creditos.stream().map(CreditoCalculado::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if ("SOMENTE_TOTAL".equals(momento) && modo != null) {
                totalCreditos = totalSemArredondamento.setScale(CASAS_MONETARIAS, modo);
            }
        }
        BigDecimal diferenca = totalCreditos == null ? null : totalCreditos.subtract(totalSemArredondamento);

        boolean bloqueado = pendencias.stream().anyMatch(Pendencia::bloqueante);
        BigDecimal custo = bloqueado || totalCreditos == null || valorOperacao == null
                ? null : valorOperacao.subtract(totalCreditos);
        return new ResultadoCalculo(valorOperacao, base, creditos, totalSemArredondamento, totalCreditos,
                diferenca, custo, List.copyOf(pendencias));
    }

    private static boolean precisaArredondar(BigDecimal valor) {
        return valor.stripTrailingZeros().scale() > CASAS_MONETARIAS;
    }

    private static RoundingMode modo(String criterio, List<Pendencia> pendencias) {
        if (criterio == null) {
            pendencias.add(new Pendencia(TipoPendencia.PARAMETRO_NAO_DEFINIDO,
                    "Há créditos com mais de 2 casas decimais e o parâmetro CRITERIO_ARREDONDAMENTO não está definido."));
            return null;
        }
        return "MEIO_PARA_CIMA".equals(criterio) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    private static String momento(String arredondamento, List<Pendencia> pendencias) {
        if (arredondamento == null) {
            pendencias.add(new Pendencia(TipoPendencia.PARAMETRO_NAO_DEFINIDO,
                    "Há créditos com mais de 2 casas decimais e o parâmetro ARREDONDAMENTO_CREDITOS não está definido."));
        }
        return arredondamento;
    }

    private static boolean usaRegraCombinada(SelecaoTributo selecao) {
        return selecao.situacao() == SelecaoTributo.Situacao.APLICAVEL
                && selecao.regra().getTributo() == Tributo.PIS_COFINS;
    }

    private static SelecaoTributo selecao(List<SelecaoTributo> selecoes, Tributo tributo) {
        return selecoes.stream().filter(s -> s.tributo() == tributo).findFirst().orElseThrow();
    }

    private static String nomes(List<RegraTributaria> regras) {
        return String.join(", ", regras.stream().map(r -> "\"" + r.getNome() + "\"").toList());
    }
}
