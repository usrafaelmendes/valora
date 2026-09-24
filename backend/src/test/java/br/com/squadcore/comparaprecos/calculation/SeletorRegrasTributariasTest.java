package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.calculation.SelecaoTributo.Situacao;
import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seleção das regras configuradas por tributo. Os percentuais abaixo são dados de teste
 * (alguns iguais ao baseline documentado); o seletor não conhece nenhum valor tributário.
 */
class SeletorRegrasTributariasTest {

    private final SeletorRegrasTributarias seletor = new SeletorRegrasTributarias();
    private final AtomicLong sequencia = new AtomicLong();

    private static final OperacaoTributavel VAZIA = new OperacaoTributavel(null, null, null, null, null, null, null);

    @Test
    void ct17_semRegraConfiguradaNaoInventaCredito() {
        List<SelecaoTributo> resultado = seletor.selecionar(VAZIA, List.of());

        assertThat(resultado).extracting(SelecaoTributo::tributo)
                .containsExactly(Tributo.ICMS, Tributo.IPI, Tributo.PIS, Tributo.COFINS);
        assertThat(resultado).allSatisfy(selecao -> {
            assertThat(selecao.situacao()).isEqualTo(Situacao.SEM_REGRA);
            assertThat(selecao.regras()).isEmpty();
            assertThat(selecao.regra()).isNull();
        });
    }

    @Test
    void regraSemCondicaoSeAplicaAQualquerOperacao() {
        RegraTributaria icms = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);

        assertThat(selecao(VAZIA, Tributo.ICMS, icms).regra()).isSameAs(icms);
    }

    @Test
    void ct13_regraSemCreditoEAplicavelEDiferenteDeNaoTerRegra() {
        RegraTributaria semCredito = regra(Tributo.IPI, FormaAliquota.SEM_CREDITO);

        SelecaoTributo selecao = selecao(VAZIA, Tributo.IPI, semCredito);

        assertThat(selecao.situacao()).isEqualTo(Situacao.APLICAVEL);
        assertThat(selecao.regra().getFormaAliquota()).isEqualTo(FormaAliquota.SEM_CREDITO);
    }

    @Test
    void regraDesativadaEIgnorada() {
        RegraTributaria icms = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        icms.setAtiva(false);

        assertThat(selecao(VAZIA, Tributo.ICMS, icms).situacao()).isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void ct33_regraDeIpiEscolhidaPeloTipoDoFornecedor() {
        RegraTributaria fabricante = regra(Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE);
        fabricante.setTipoFornecedor(TipoFornecedor.FABRICANTE);
        RegraTributaria atacadista = regra(Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE);
        atacadista.setTipoFornecedor(TipoFornecedor.ATACADISTA);
        atacadista.setFator(new BigDecimal("0.25"));

        assertThat(selecao(operacaoDoTipo(TipoFornecedor.FABRICANTE), Tributo.IPI, fabricante, atacadista).regra())
                .isSameAs(fabricante);
        assertThat(selecao(operacaoDoTipo(TipoFornecedor.ATACADISTA), Tributo.IPI, fabricante, atacadista).regra())
                .isSameAs(atacadista);
        // Tipo desconhecido: nenhuma das regras condicionadas se aplica.
        assertThat(selecao(VAZIA, Tributo.IPI, fabricante, atacadista).situacao()).isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void ct16_abrangenciaInterestadualComparaOrigemEDestino() {
        RegraTributaria interestadual = regra(Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        interestadual.setAbrangenciaUf(AbrangenciaUf.INTERESTADUAL);
        RegraTributaria interna = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        interna.setAbrangenciaUf(AbrangenciaUf.INTERNA);

        assertThat(selecao(operacaoEntre(Uf.ES, Uf.GO), Tributo.ICMS, interestadual, interna).regra())
                .isSameAs(interestadual);
        assertThat(selecao(operacaoEntre(Uf.GO, Uf.GO), Tributo.ICMS, interestadual, interna).regra())
                .isSameAs(interna);
        assertThat(selecao(operacaoEntre(null, Uf.GO), Tributo.ICMS, interestadual, interna).situacao())
                .isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void ufsDeOrigemEDestinoEspecificas() {
        RegraTributaria esParaGo = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        esParaGo.setUfOrigem(Uf.ES);
        esParaGo.setUfDestino(Uf.GO);

        assertThat(selecao(operacaoEntre(Uf.ES, Uf.GO), Tributo.ICMS, esParaGo).situacao()).isEqualTo(Situacao.APLICAVEL);
        assertThat(selecao(operacaoEntre(Uf.MG, Uf.GO), Tributo.ICMS, esParaGo).situacao()).isEqualTo(Situacao.SEM_REGRA);
        assertThat(selecao(operacaoEntre(Uf.ES, Uf.RJ), Tributo.ICMS, esParaGo).situacao()).isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void ct31_origemDaMercadoriaConfiguradaPelaLista() {
        RegraTributaria porOrigem = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        porOrigem.definirOrigensMercadoria(Set.of("1", "2"));

        assertThat(selecao(operacaoDeOrigem("1"), Tributo.ICMS, porOrigem).situacao()).isEqualTo(Situacao.APLICAVEL);
        assertThat(selecao(operacaoDeOrigem("0"), Tributo.ICMS, porOrigem).situacao()).isEqualTo(Situacao.SEM_REGRA);
        assertThat(selecao(operacaoDeOrigem(null), Tributo.ICMS, porOrigem).situacao()).isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void cfopConfiguradoPelaLista() {
        RegraTributaria regra = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        regra.definirCfops(Set.of("1111", "2222"));

        assertThat(selecao(new OperacaoTributavel(null, null, null, null, null, null, "2222"), Tributo.ICMS, regra)
                .situacao()).isEqualTo(Situacao.APLICAVEL);
        assertThat(selecao(new OperacaoTributavel(null, null, null, null, null, null, "3333"), Tributo.ICMS, regra)
                .situacao()).isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void fornecedorEProdutoEspecificos() {
        Fornecedor fornecedor = new Fornecedor("Fornecedor Teste", "11222333000181", Uf.GO, TipoFornecedor.FABRICANTE, null);
        ReflectionTestUtils.setField(fornecedor, "id", 10L);
        Produto produto = new Produto("Produto Teste", null, null);
        ReflectionTestUtils.setField(produto, "id", 20L);
        RegraTributaria regra = regra(Tributo.IPI, FormaAliquota.PERCENTUAL_FIXO);
        regra.setFornecedor(fornecedor);
        regra.setProduto(produto);

        assertThat(selecao(new OperacaoTributavel(10L, null, 20L, null, null, null, null), Tributo.IPI, regra)
                .situacao()).isEqualTo(Situacao.APLICAVEL);
        assertThat(selecao(new OperacaoTributavel(10L, null, 21L, null, null, null, null), Tributo.IPI, regra)
                .situacao()).isEqualTo(Situacao.SEM_REGRA);
        assertThat(selecao(new OperacaoTributavel(11L, null, 20L, null, null, null, null), Tributo.IPI, regra)
                .situacao()).isEqualTo(Situacao.SEM_REGRA);
    }

    @Test
    void maiorPrioridadeVence() {
        RegraTributaria geral = regra(Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        RegraTributaria especifica = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        especifica.setPrioridade(10);

        SelecaoTributo selecao = selecao(VAZIA, Tributo.ICMS, geral, especifica);

        assertThat(selecao.situacao()).isEqualTo(Situacao.APLICAVEL);
        assertThat(selecao.regra()).isSameAs(especifica);
    }

    @Test
    void empateNaMaiorPrioridadeEConflitoSemEscolha() {
        RegraTributaria primeira = regra(Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        RegraTributaria segunda = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        RegraTributaria menor = regra(Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        menor.setPrioridade(-1);

        SelecaoTributo selecao = selecao(VAZIA, Tributo.ICMS, segunda, menor, primeira);

        assertThat(selecao.situacao()).isEqualTo(Situacao.CONFLITO);
        assertThat(selecao.regra()).isNull();
        assertThat(selecao.regras()).containsExactly(primeira, segunda);
    }

    @Test
    void pisCofinsCombinadoCobreOsDoisTributos() {
        RegraTributaria combinada = regra(Tributo.PIS_COFINS, FormaAliquota.PERCENTUAL_FIXO);

        List<SelecaoTributo> resultado = seletor.selecionar(VAZIA, List.of(combinada));

        assertThat(resultado.get(2).regra()).isSameAs(combinada);
        assertThat(resultado.get(3).regra()).isSameAs(combinada);
    }

    @Test
    void pisSeparadoConcorreComOCombinadoPelaPrioridade() {
        RegraTributaria combinada = regra(Tributo.PIS_COFINS, FormaAliquota.PERCENTUAL_FIXO);
        RegraTributaria pis = regra(Tributo.PIS, FormaAliquota.PERCENTUAL_FIXO);

        List<SelecaoTributo> empate = seletor.selecionar(VAZIA, List.of(combinada, pis));
        assertThat(empate.get(2).situacao()).isEqualTo(Situacao.CONFLITO);
        assertThat(empate.get(3).regra()).isSameAs(combinada);

        pis.setPrioridade(1);
        List<SelecaoTributo> resolvido = seletor.selecionar(VAZIA, List.of(combinada, pis));
        assertThat(resolvido.get(2).regra()).isSameAs(pis);
        assertThat(resolvido.get(3).regra()).isSameAs(combinada);
    }

    private SelecaoTributo selecao(OperacaoTributavel operacao, Tributo tributo, RegraTributaria... regras) {
        return seletor.selecionar(operacao, List.of(regras)).stream()
                .filter(selecao -> selecao.tributo() == tributo)
                .findFirst().orElseThrow();
    }

    private RegraTributaria regra(Tributo tributo, FormaAliquota forma) {
        RegraTributaria regra = new RegraTributaria("Regra de teste " + sequencia.incrementAndGet(), tributo, forma);
        ReflectionTestUtils.setField(regra, "id", sequencia.get());
        return regra;
    }

    private static OperacaoTributavel operacaoDoTipo(TipoFornecedor tipo) {
        return new OperacaoTributavel(null, tipo, null, null, null, null, null);
    }

    private static OperacaoTributavel operacaoEntre(Uf origem, Uf destino) {
        return new OperacaoTributavel(null, null, null, origem, destino, null, null);
    }

    private static OperacaoTributavel operacaoDeOrigem(String origem) {
        return new OperacaoTributavel(null, null, null, null, null, origem, null);
    }
}
