package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.ComponenteValor;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cálculo do custo efetivo (CT12 a CT17, CT31, CT33, CT36).
 *
 * As alíquotas das regras são DADOS DE TESTE fictícios configurados em cada caso; a calculadora não conhece nenhum valor tributário. Onde a
 * documentação deixa o arredondamento pendente, os testes verificam o comportamento de cada
 * opção configurável, e não uma regra de negócio.
 */
class CalculadoraCustoEfetivoTest {

    private static final BigDecimal VALOR_325 = new BigDecimal("325.00");

    private final CalculadoraCustoEfetivo calculadora = new CalculadoraCustoEfetivo(new SeletorRegrasTributarias());
    private final AtomicLong sequencia = new AtomicLong();

    // ---- Fórmula (CT12, CT36, CT13, CT14) ----

    @Test
    void ct12_ct36_creditosIndividuaisSobreOValorDaOperacaoSemCascata() {
        List<RegraTributaria> regras = List.of(
                fixa(Tributo.ICMS, "7"), fixa(Tributo.IPI, "10"), fixa(Tributo.PIS_COFINS, "6.35"));

        ResultadoCalculo r = calcular(operacao(), regras, config("POR_CREDITO", "MEIO_PARA_CIMA"));

        assertThat(r.creditos()).extracting(CreditoCalculado::tributo)
                .containsExactly(Tributo.ICMS, Tributo.IPI, Tributo.PIS_COFINS);
        assertThat(r.creditos()).extracting(CreditoCalculado::valor)
                .containsExactly(new BigDecimal("22.75"), new BigDecimal("32.50"), new BigDecimal("20.64"));
        assertThat(r.creditos().get(2).valorSemArredondamento()).isEqualByComparingTo("20.6375");
        assertThat(r.totalCreditos()).isEqualByComparingTo("75.89");
        assertThat(r.custoEfetivo()).isEqualByComparingTo("249.11");
        // CT36: em cascata daria 254,75.
        assertThat(r.custoEfetivo()).isNotEqualByComparingTo("254.75");
        assertThat(r.pendencias()).isEmpty();
        assertThat(r.completo()).isTrue();
    }

    @Test
    void ct12_arredondamentoSomenteNoTotalMostraCreditosSemArredondarEADiferenca() {
        List<RegraTributaria> regras = List.of(
                fixa(Tributo.ICMS, "7"), fixa(Tributo.IPI, "10"), fixa(Tributo.PIS_COFINS, "6.35"));

        ResultadoCalculo r = calcular(operacao(), regras, config("SOMENTE_TOTAL", "MEIO_PARA_CIMA"));

        assertThat(r.creditos()).extracting(CreditoCalculado::valor).containsOnlyNulls();
        assertThat(r.totalCreditosSemArredondamento()).isEqualByComparingTo("75.8875");
        assertThat(r.totalCreditos()).isEqualByComparingTo("75.89");
        assertThat(r.diferencaArredondamento()).isEqualByComparingTo("0.0025");
        assertThat(r.custoEfetivo()).isEqualByComparingTo("249.11");
    }

    @Test
    void calculoSimplesComUmCredito() {
        List<RegraTributaria> regras = List.of(fixa(Tributo.ICMS, "7"), semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS));

        ResultadoCalculo r = calcular(operacao(), regras, config(null, null));

        assertThat(r.creditos().getFirst().valor()).isEqualByComparingTo("22.75");
        assertThat(r.totalCreditos()).isEqualByComparingTo("22.75");
        assertThat(r.custoEfetivo()).isEqualByComparingTo("302.25");
    }

    @Test
    void ct13_regrasSemCreditoResultamEmCustoIgualAoValorDaOperacao() {
        List<RegraTributaria> regras = List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS));

        ResultadoCalculo r = calcular(operacao(), regras, config(null, null));

        assertThat(r.creditos()).extracting(CreditoCalculado::situacao).containsOnly(SituacaoCredito.SEM_CREDITO);
        assertThat(r.totalCreditos()).isEqualByComparingTo("0.00");
        assertThat(r.custoEfetivo()).isEqualByComparingTo("325.00");
        assertThat(r.pendencias()).isEmpty();
    }

    @Test
    void ct17_semRegraNaoPresumeCreditoZeroEDeixaOCustoPendente() {
        ResultadoCalculo r = calcular(operacao(), List.of(), config("POR_CREDITO", "MEIO_PARA_CIMA"));

        assertThat(r.completo()).isFalse();
        assertThat(r.custoEfetivo()).isNull();
        assertThat(r.totalCreditos()).isNull();
        assertThat(r.creditos()).extracting(CreditoCalculado::situacao).containsOnly(SituacaoCredito.REGRA_AUSENTE);
        assertThat(r.creditos()).extracting(CreditoCalculado::valorSemArredondamento).containsOnlyNulls();
        assertThat(r.pendencias()).extracting(Pendencia::tipo).containsOnly(TipoPendencia.REGRA_AUSENTE).hasSize(4);
        assertThat(r.valorOperacao()).isEqualByComparingTo("325.00");
    }

    @Test
    void regraAusenteEmUmTributoBloqueiaMesmoComOsDemaisCalculados() {
        List<RegraTributaria> regras = List.of(fixa(Tributo.ICMS, "7"), fixa(Tributo.PIS_COFINS, "6.35"));

        ResultadoCalculo r = calcular(operacao(), regras, config("POR_CREDITO", "MEIO_PARA_CIMA"));

        assertThat(r.custoEfetivo()).isNull();
        assertThat(r.creditos().get(0).valor()).isEqualByComparingTo("22.75");
        assertThat(r.creditos().get(1).situacao()).isEqualTo(SituacaoCredito.REGRA_AUSENTE);
        assertThat(r.pendencias()).extracting(Pendencia::mensagem).anyMatch(m -> m.contains("IPI"));
    }

    // ---- Seleção: desativada, conflito, prioridade, fornecedor e produto ----

    @Test
    void regraDesativadaNaoEUsada() {
        RegraTributaria icms = fixa(Tributo.ICMS, "7");
        icms.setAtiva(false);

        ResultadoCalculo r = calcular(operacao(), List.of(icms, semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS)), config(null, null));

        assertThat(r.creditos().getFirst().situacao()).isEqualTo(SituacaoCredito.REGRA_AUSENTE);
        assertThat(r.custoEfetivo()).isNull();
    }

    @Test
    void conflitoEntreRegrasNaoEscolheNenhumaERegistraAsEmpatadas() {
        RegraTributaria a = fixa(Tributo.ICMS, "7");
        RegraTributaria b = fixa(Tributo.ICMS, "11");

        ResultadoCalculo r = calcular(operacao(), List.of(a, b, semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS)), config(null, null));

        CreditoCalculado icms = r.creditos().getFirst();
        assertThat(icms.situacao()).isEqualTo(SituacaoCredito.CONFLITO_REGRAS);
        assertThat(icms.regra()).isNull();
        assertThat(icms.regrasEmConflito()).containsExactly(a, b);
        assertThat(r.pendencias()).extracting(Pendencia::tipo).containsExactly(TipoPendencia.CONFLITO_REGRAS);
        assertThat(r.custoEfetivo()).isNull();
    }

    @Test
    void prioridadeMaiorResolveOConflito() {
        RegraTributaria geral = fixa(Tributo.ICMS, "11");
        RegraTributaria prioritaria = fixa(Tributo.ICMS, "7");
        prioritaria.setPrioridade(10);

        ResultadoCalculo r = calcular(operacao(), List.of(geral, prioritaria, semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS)), config(null, null));

        assertThat(r.creditos().getFirst().regra()).isSameAs(prioritaria);
        assertThat(r.creditos().getFirst().valor()).isEqualByComparingTo("22.75");
    }

    @Test
    void regraEspecificaPorFornecedorEPorProduto() {
        RegraTributaria geral = fixa(Tributo.ICMS, "11");
        RegraTributaria doFornecedor = fixa(Tributo.ICMS, "9");
        doFornecedor.setFornecedor(fornecedor(10L));
        doFornecedor.setPrioridade(1);
        RegraTributaria doProduto = fixa(Tributo.ICMS, "7");
        doProduto.setProduto(produto(20L));
        doProduto.setPrioridade(2);
        List<RegraTributaria> regras = List.of(geral, doFornecedor, doProduto, semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS));

        assertThat(calcular(operacao(10L, 20L), regras, config(null, null)).creditos().getFirst().regra())
                .isSameAs(doProduto);
        assertThat(calcular(operacao(10L, 21L), regras, config(null, null)).creditos().getFirst().regra())
                .isSameAs(doFornecedor);
        assertThat(calcular(operacao(11L, 21L), regras, config(null, null)).creditos().getFirst().regra())
                .isSameAs(geral);
    }

    // ---- IPI: fabricante e atacadista (CT15, CT33) ----

    @Test
    void ct33_ipiDoFabricanteUsaAliquotaDaOperacaoIntegral() {
        ResultadoCalculo r = calcular(operacao(TipoFornecedor.FABRICANTE), regrasIpiPorTipo(),
                config(null, null), new AliquotasOperacao(null, new BigDecimal("10"), null, null));

        CreditoCalculado ipi = r.creditos().get(1);
        assertThat(ipi.aliquotaObtida()).isEqualByComparingTo("10");
        assertThat(ipi.fator()).isEqualByComparingTo("1");
        assertThat(ipi.valor()).isEqualByComparingTo("32.50");
        assertThat(r.custoEfetivo()).isEqualByComparingTo("292.50");
    }

    @Test
    void ct15_ct33_atacadistaAplicaOFatorConfiguradoSobreAAliquota() {
        ResultadoCalculo r = calcular(operacao(TipoFornecedor.ATACADISTA), regrasIpiPorTipo(),
                config("POR_CREDITO", "MEIO_PARA_CIMA"), new AliquotasOperacao(null, new BigDecimal("10"), null, null));

        CreditoCalculado ipi = r.creditos().get(1);
        assertThat(ipi.fator()).isEqualByComparingTo("0.25");
        assertThat(ipi.aliquotaAplicada()).isEqualByComparingTo("2.5");
        assertThat(ipi.valorSemArredondamento()).isEqualByComparingTo("8.125");
        assertThat(ipi.valor()).isEqualByComparingTo("8.13");
    }

    @Test
    void ct33_arredondamentoDoAtacadistaDependeDoCriterioConfigurado() {
        ResultadoCalculo r = calcular(operacao(TipoFornecedor.ATACADISTA), regrasIpiPorTipo(),
                config("POR_CREDITO", "MEIO_PARA_PAR"), new AliquotasOperacao(null, new BigDecimal("10"), null, null));

        assertThat(r.creditos().get(1).valor()).isEqualByComparingTo("8.12");
        assertThat(r.diferencaArredondamento()).isEqualByComparingTo("-0.005");
    }

    @Test
    void arredondamentoNecessarioENaoConfiguradoFicaPendenteSemEsconderOValor() {
        ResultadoCalculo r = calcular(operacao(TipoFornecedor.ATACADISTA), regrasIpiPorTipo(),
                config(null, null), new AliquotasOperacao(null, new BigDecimal("10"), null, null));

        assertThat(r.custoEfetivo()).isNull();
        assertThat(r.totalCreditos()).isNull();
        assertThat(r.creditos().get(1).valorSemArredondamento()).isEqualByComparingTo("8.125");
        assertThat(r.totalCreditosSemArredondamento()).isEqualByComparingTo("8.125");
        assertThat(r.pendencias()).extracting(Pendencia::mensagem)
                .anyMatch(m -> m.contains("CRITERIO_ARREDONDAMENTO"))
                .anyMatch(m -> m.contains("ARREDONDAMENTO_CREDITOS"));
    }

    @Test
    void semNecessidadeDeArredondarOsParametrosDeArredondamentoNaoSaoExigidos() {
        ResultadoCalculo r = calcular(operacao(TipoFornecedor.FABRICANTE), regrasIpiPorTipo(),
                config(null, null), new AliquotasOperacao(null, new BigDecimal("10"), null, null));

        assertThat(r.completo()).isTrue();
        assertThat(r.diferencaArredondamento()).isEqualByComparingTo("0");
    }

    // ---- ICMS: interestadual, por origem da mercadoria e por fornecedor (CT16, CT31) ----

    @Test
    void ct16_interestadualUsaAAliquotaDaOperacao() {
        RegraTributaria interestadual = regra(Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE, null);
        interestadual.setAbrangenciaUf(AbrangenciaUf.INTERESTADUAL);

        ResultadoCalculo interestadualEs = calcular(operacaoUf(Uf.ES, Uf.GO, "0"),
                List.of(interestadual, semCredito(Tributo.IPI), semCredito(Tributo.PIS_COFINS)),
                config(null, null), new AliquotasOperacao(new BigDecimal("11"), null, null, null));
        ResultadoCalculo interna = calcular(operacaoUf(Uf.GO, Uf.GO, "0"),
                List.of(interestadual, semCredito(Tributo.IPI), semCredito(Tributo.PIS_COFINS)),
                config(null, null), new AliquotasOperacao(new BigDecimal("18"), null, null, null));

        assertThat(interestadualEs.creditos().getFirst().valor()).isEqualByComparingTo("35.75");
        assertThat(interna.creditos().getFirst().situacao()).isEqualTo(SituacaoCredito.REGRA_AUSENTE);
    }

    @Test
    void ct31_regraPorOrigemDaMercadoriaPrevaleceSobreInterestadualPelaPrioridade() {
        RegraTributaria interestadual = regra(Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE, null);
        interestadual.setAbrangenciaUf(AbrangenciaUf.INTERESTADUAL);
        RegraTributaria porOrigem = fixa(Tributo.ICMS, "5");
        porOrigem.definirOrigensMercadoria(Set.of("1", "2"));
        porOrigem.setPrioridade(10);
        List<RegraTributaria> regras = List.of(interestadual, porOrigem, semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS));
        AliquotasOperacao doXml = new AliquotasOperacao(new BigDecimal("11"), null, null, null);

        CreditoCalculado estrangeiro = calcular(operacaoUf(Uf.ES, Uf.GO, "1"), regras, config(null, null), doXml)
                .creditos().getFirst();
        CreditoCalculado nacional = calcular(operacaoUf(Uf.ES, Uf.GO, "0"), regras, config(null, null), doXml)
                .creditos().getFirst();

        assertThat(estrangeiro.regra()).isSameAs(porOrigem);
        assertThat(estrangeiro.valor()).isEqualByComparingTo("16.25");
        assertThat(nacional.regra()).isSameAs(interestadual);
        assertThat(nacional.valor()).isEqualByComparingTo("35.75");
    }

    @Test
    void regraPorOrigemEInterestadualComMesmaPrioridadeGeramConflito() {
        RegraTributaria interestadual = regra(Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE, null);
        interestadual.setAbrangenciaUf(AbrangenciaUf.INTERESTADUAL);
        RegraTributaria porOrigem = fixa(Tributo.ICMS, "5");
        porOrigem.definirOrigensMercadoria(Set.of("1"));

        ResultadoCalculo r = calcular(operacaoUf(Uf.ES, Uf.GO, "1"), List.of(interestadual, porOrigem,
                semCredito(Tributo.IPI), semCredito(Tributo.PIS_COFINS)), config(null, null),
                new AliquotasOperacao(new BigDecimal("11"), null, null, null));

        assertThat(r.creditos().getFirst().situacao()).isEqualTo(SituacaoCredito.CONFLITO_REGRAS);
        assertThat(r.custoEfetivo()).isNull();
    }

    @Test
    void regrasDoFornecedorComIcmsFixoEIpiIntegralPrevalecemPelaPrioridade() {
        Fornecedor especifico = fornecedor(30L);
        RegraTributaria icmsFornecedor = fixa(Tributo.ICMS, "11");
        icmsFornecedor.setFornecedor(especifico);
        icmsFornecedor.setPrioridade(10);
        RegraTributaria ipiFornecedor = regra(Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, null);
        ipiFornecedor.setFornecedor(especifico);
        ipiFornecedor.setPrioridade(10);
        List<RegraTributaria> regras = new ArrayList<>(regrasIpiPorTipo());
        regras.addAll(List.of(icmsFornecedor, ipiFornecedor, semCredito(Tributo.PIS_COFINS)));

        ResultadoCalculo r = calcular(
                new OperacaoTributavel(30L, TipoFornecedor.ATACADISTA, null, Uf.MT, Uf.GO, "0", null),
                regras, config(null, null), new AliquotasOperacao(null, new BigDecimal("10"), null, null));

        assertThat(r.creditos().get(0).valor()).isEqualByComparingTo("35.75");
        // Prioridade da regra do fornecedor sobre a do atacadista: crédito integral.
        assertThat(r.creditos().get(1).regra()).isSameAs(ipiFornecedor);
        assertThat(r.creditos().get(1).valor()).isEqualByComparingTo("32.50");
    }

    @Test
    void aliquotaDaOperacaoIndisponivelNaoEPresumida() {
        ResultadoCalculo r = calcular(operacao(TipoFornecedor.FABRICANTE), regrasIpiPorTipo(),
                config(null, null), AliquotasOperacao.NENHUMA);

        CreditoCalculado ipi = r.creditos().get(1);
        assertThat(ipi.situacao()).isEqualTo(SituacaoCredito.ALIQUOTA_INDISPONIVEL);
        assertThat(ipi.valorSemArredondamento()).isNull();
        assertThat(r.pendencias()).extracting(Pendencia::tipo).contains(TipoPendencia.ALIQUOTA_INDISPONIVEL);
        assertThat(r.custoEfetivo()).isNull();
    }

    // ---- PIS/COFINS: combinado ou separados ----

    @Test
    void pisCofinsCombinadoComAliquotaDaOperacaoSomaPisECofinsUmaUnicaVez() {
        RegraTributaria combinada = regra(Tributo.PIS_COFINS, FormaAliquota.ALIQUOTA_DA_NFE, null);

        ResultadoCalculo r = calcular(operacao(), List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI), combinada),
                config("POR_CREDITO", "MEIO_PARA_CIMA"),
                new AliquotasOperacao(null, null, new BigDecimal("1.35"), new BigDecimal("5.00")));

        assertThat(r.creditos()).hasSize(3);
        assertThat(r.creditos().get(2).aliquotaObtida()).isEqualByComparingTo("6.35");
        assertThat(r.creditos().get(2).valor()).isEqualByComparingTo("20.64");
    }

    @Test
    void pisECofinsSeparadosSaoCalculadosIndividualmente() {
        List<RegraTributaria> regras = List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI),
                fixa(Tributo.PIS, "1.35"), fixa(Tributo.COFINS, "5.00"));

        ResultadoCalculo r = calcular(operacao(), regras, config("POR_CREDITO", "MEIO_PARA_CIMA"));

        assertThat(r.creditos()).extracting(CreditoCalculado::tributo)
                .containsExactly(Tributo.ICMS, Tributo.IPI, Tributo.PIS, Tributo.COFINS);
        assertThat(r.creditos().get(2).valorSemArredondamento()).isEqualByComparingTo("4.3875");
        assertThat(r.creditos().get(2).valor()).isEqualByComparingTo("4.39");
        assertThat(r.creditos().get(3).valor()).isEqualByComparingTo("16.25");
        assertThat(r.totalCreditos()).isEqualByComparingTo("20.64");
    }

    @Test
    void combinadoEscolhidoParaSoUmDosTributosEConfiguracaoInconsistente() {
        RegraTributaria cofins = fixa(Tributo.COFINS, "5.00");
        cofins.setPrioridade(5);
        List<RegraTributaria> regras = List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI),
                fixa(Tributo.PIS_COFINS, "6.35"), cofins);

        ResultadoCalculo r = calcular(operacao(), regras, config("POR_CREDITO", "MEIO_PARA_CIMA"));

        assertThat(r.creditos().get(2).tributo()).isEqualTo(Tributo.PIS_COFINS);
        assertThat(r.creditos().get(2).situacao()).isEqualTo(SituacaoCredito.CONFIGURACAO_INCONSISTENTE);
        assertThat(r.pendencias()).extracting(Pendencia::tipo).containsExactly(TipoPendencia.CONFIGURACAO_INCONSISTENTE);
        assertThat(r.custoEfetivo()).isNull();
    }

    // ---- Valor da operação e base ----

    @Test
    void composicaoDoValorDaOperacaoEBaseDiferenteConfiguradas() {
        Map<ComponenteValor, BigDecimal> componentes = new EnumMap<>(ComponenteValor.class);
        componentes.put(ComponenteValor.VALOR_PRODUTO, new BigDecimal("200.00"));
        componentes.put(ComponenteValor.IPI, new BigDecimal("30.00"));
        componentes.put(ComponenteValor.FRETE, new BigDecimal("10.00"));
        componentes.put(ComponenteValor.DESCONTO, new BigDecimal("5.00"));
        ConfiguracaoCalculo config = new ConfiguracaoCalculo(
                List.of(ComponenteValor.VALOR_PRODUTO, ComponenteValor.IPI, ComponenteValor.FRETE, ComponenteValor.DESCONTO),
                false, List.of(ComponenteValor.VALOR_PRODUTO), null, null);

        ResultadoCalculo r = calculadora.calcular(new EntradaCalculo(operacao(), componentes, AliquotasOperacao.NENHUMA,
                config, List.of(fixa(Tributo.ICMS, "7"), semCredito(Tributo.IPI), semCredito(Tributo.PIS_COFINS)),
                List.of()));

        assertThat(r.valorOperacao()).isEqualByComparingTo("235.00");
        assertThat(r.baseCreditos()).isEqualByComparingTo("200.00");
        assertThat(r.creditos().getFirst().base()).isEqualByComparingTo("200.00");
        assertThat(r.creditos().getFirst().valor()).isEqualByComparingTo("14.00");
        assertThat(r.custoEfetivo()).isEqualByComparingTo("221.00");
    }

    @Test
    void composicaoNaoDefinidaFicaPendente() {
        ConfiguracaoCalculo config = new ConfiguracaoCalculo(null, false, null, null, null);

        ResultadoCalculo r = calculadora.calcular(new EntradaCalculo(operacao(), componentes325(),
                AliquotasOperacao.NENHUMA, config, List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS)), List.of()));

        assertThat(r.valorOperacao()).isNull();
        assertThat(r.baseCreditos()).isNull();
        assertThat(r.pendencias()).extracting(Pendencia::mensagem)
                .anyMatch(m -> m.contains("COMPOSICAO_VALOR_OPERACAO"))
                .anyMatch(m -> m.contains("COMPOSICAO_BASE_CREDITOS"));
        assertThat(r.custoEfetivo()).isNull();
    }

    @Test
    void componenteIndisponivelNaFonteNaoEPresumidoZero() {
        ConfiguracaoCalculo config = new ConfiguracaoCalculo(
                List.of(ComponenteValor.VALOR_PRODUTO, ComponenteValor.FRETE), true, null, null, null);

        ResultadoCalculo r = calculadora.calcular(new EntradaCalculo(operacao(), componentes325(),
                AliquotasOperacao.NENHUMA, config, List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS)), List.of()));

        assertThat(r.valorOperacao()).isNull();
        assertThat(r.pendencias()).extracting(Pendencia::tipo).containsExactly(TipoPendencia.DADO_INDISPONIVEL);
        assertThat(r.custoEfetivo()).isNull();
    }

    @Test
    void valorDaOperacaoNegativoEInvalido() {
        Map<ComponenteValor, BigDecimal> componentes = new EnumMap<>(ComponenteValor.class);
        componentes.put(ComponenteValor.VALOR_PRODUTO, new BigDecimal("10.00"));
        componentes.put(ComponenteValor.DESCONTO, new BigDecimal("20.00"));
        ConfiguracaoCalculo config = new ConfiguracaoCalculo(
                List.of(ComponenteValor.VALOR_PRODUTO, ComponenteValor.DESCONTO), true, null, null, null);

        ResultadoCalculo r = calculadora.calcular(new EntradaCalculo(operacao(), componentes,
                AliquotasOperacao.NENHUMA, config, List.of(), List.of()));

        assertThat(r.pendencias()).extracting(Pendencia::tipo).contains(TipoPendencia.VALOR_INVALIDO);
        assertThat(r.valorOperacao()).isNull();
    }

    // ---- Pendências prévias e rastreabilidade ----

    @Test
    void pendenciaPreviaBloqueanteImpedeOCustoEAvisoNao() {
        List<RegraTributaria> regras = List.of(semCredito(Tributo.ICMS), semCredito(Tributo.IPI),
                semCredito(Tributo.PIS_COFINS));
        Pendencia aviso = new Pendencia(TipoPendencia.CFOPS_PARTICIPANTES_NAO_DEFINIDOS, "aviso");
        Pendencia bloqueante = new Pendencia(TipoPendencia.PARAMETRO_NAO_DEFINIDO, "falta parâmetro");

        ResultadoCalculo comAviso = calculadora.calcular(new EntradaCalculo(operacao(), componentes325(),
                AliquotasOperacao.NENHUMA, config(null, null), regras, List.of(aviso)));
        ResultadoCalculo comBloqueio = calculadora.calcular(new EntradaCalculo(operacao(), componentes325(),
                AliquotasOperacao.NENHUMA, config(null, null), regras, List.of(aviso, bloqueante)));

        assertThat(comAviso.custoEfetivo()).isEqualByComparingTo("325.00");
        assertThat(comAviso.pendencias()).containsExactly(aviso);
        assertThat(comBloqueio.custoEfetivo()).isNull();
        assertThat(comBloqueio.totalCreditos()).isEqualByComparingTo("0.00");
    }

    @Test
    void cadaCreditoRegistraRegraAliquotaFatorEBase() {
        RegraTributaria ipi = regra(Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, "0.25");

        ResultadoCalculo r = calcular(operacao(), List.of(semCredito(Tributo.ICMS), ipi, semCredito(Tributo.PIS_COFINS)),
                config("POR_CREDITO", "MEIO_PARA_CIMA"), new AliquotasOperacao(null, new BigDecimal("20"), null, null));

        CreditoCalculado credito = r.creditos().get(1);
        assertThat(credito.regra()).isSameAs(ipi);
        assertThat(credito.aliquotaObtida()).isEqualByComparingTo("20");
        assertThat(credito.fator()).isEqualByComparingTo("0.25");
        assertThat(credito.aliquotaAplicada()).isEqualByComparingTo("5");
        assertThat(credito.base()).isEqualByComparingTo("325.00");
        assertThat(credito.valor()).isEqualByComparingTo("16.25");
    }

    // ---- Apoio ----

    private ResultadoCalculo calcular(OperacaoTributavel operacao, List<RegraTributaria> regras, ConfiguracaoCalculo config) {
        return calcular(operacao, regras, config, AliquotasOperacao.NENHUMA);
    }

    private ResultadoCalculo calcular(OperacaoTributavel operacao, List<RegraTributaria> regras,
                                      ConfiguracaoCalculo config, AliquotasOperacao aliquotas) {
        return calculadora.calcular(new EntradaCalculo(operacao, componentes325(), aliquotas, config, regras, List.of()));
    }

    /** Valor da operação = VALOR_PRODUTO, base = valor da operação; arredondamento conforme o caso. */
    private static ConfiguracaoCalculo config(String arredondamento, String criterio) {
        return new ConfiguracaoCalculo(List.of(ComponenteValor.VALOR_PRODUTO), true, null, arredondamento, criterio);
    }

    private static Map<ComponenteValor, BigDecimal> componentes325() {
        Map<ComponenteValor, BigDecimal> componentes = new EnumMap<>(ComponenteValor.class);
        componentes.put(ComponenteValor.VALOR_PRODUTO, VALOR_325);
        return componentes;
    }

    private List<RegraTributaria> regrasIpiPorTipo() {
        RegraTributaria fabricante = regra(Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, "1");
        fabricante.setTipoFornecedor(TipoFornecedor.FABRICANTE);
        RegraTributaria atacadista = regra(Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, "0.25");
        atacadista.setTipoFornecedor(TipoFornecedor.ATACADISTA);
        return List.of(semCredito(Tributo.ICMS), fabricante, atacadista, semCredito(Tributo.PIS_COFINS));
    }

    private RegraTributaria fixa(Tributo tributo, String aliquota) {
        RegraTributaria regra = regra(tributo, FormaAliquota.PERCENTUAL_FIXO, "1");
        regra.setAliquota(new BigDecimal(aliquota));
        return regra;
    }

    private RegraTributaria semCredito(Tributo tributo) {
        return regra(tributo, FormaAliquota.SEM_CREDITO, null);
    }

    private RegraTributaria regra(Tributo tributo, FormaAliquota forma, String fator) {
        RegraTributaria regra = new RegraTributaria("Regra de teste " + sequencia.incrementAndGet(), tributo, forma);
        ReflectionTestUtils.setField(regra, "id", sequencia.get());
        if (forma != FormaAliquota.SEM_CREDITO) {
            regra.setFator(new BigDecimal(fator == null ? "1" : fator));
        }
        return regra;
    }

    private static OperacaoTributavel operacao() {
        return operacao(TipoFornecedor.FABRICANTE);
    }

    private static OperacaoTributavel operacao(TipoFornecedor tipo) {
        return new OperacaoTributavel(1L, tipo, 2L, Uf.GO, Uf.GO, "0", null);
    }

    private static OperacaoTributavel operacao(Long fornecedorId, Long produtoId) {
        return new OperacaoTributavel(fornecedorId, TipoFornecedor.FABRICANTE, produtoId, Uf.GO, Uf.GO, "0", null);
    }

    private static OperacaoTributavel operacaoUf(Uf origem, Uf destino, String origemMercadoria) {
        return new OperacaoTributavel(1L, TipoFornecedor.FABRICANTE, 2L, origem, destino, origemMercadoria, null);
    }

    private static Fornecedor fornecedor(Long id) {
        Fornecedor fornecedor = new Fornecedor("Fornecedor Teste " + id, "11222333000181", Uf.MT,
                TipoFornecedor.ATACADISTA, null);
        ReflectionTestUtils.setField(fornecedor, "id", id);
        return fornecedor;
    }

    private static Produto produto(Long id) {
        Produto produto = new Produto("Produto Teste " + id, null, null);
        ReflectionTestUtils.setField(produto, "id", id);
        return produto;
    }
}
