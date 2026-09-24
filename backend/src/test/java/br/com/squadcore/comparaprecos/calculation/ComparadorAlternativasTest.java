package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.PendenciaCalculo;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.entity.SituacaoAlternativa;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classificação das opções de uma comparação (RF11, CT18 a CT20, CT35), sem banco e sem cálculo:
 * os cálculos abaixo são resultados prontos (DADOS DE TESTE), como os gravados pelo CalculoService.
 */
class ComparadorAlternativasTest {

    private static final BigDecimal QUANTIDADE = new BigDecimal("2");

    private final ComparadorAlternativas comparador = new ComparadorAlternativas();
    private long sequencia;

    @Test
    void ct18_ordenaPeloCustoEfetivoDoMenorParaOMaiorENaoPeloPrecoNominal() {
        // O menor preço nominal (300,00) tem o maior custo efetivo: fica por último.
        CandidatoComparacao a = candidato(opcao(true, "30 dias"), calculado("325.00", "249.11"));
        CandidatoComparacao b = candidato(opcao(true, null), calculado("325.00", "248.03"));
        CandidatoComparacao c = candidato(opcao(true, null), calculado("300.00", "290.00"));

        List<ResultadoComparacao> resultado = comparador.classificar(List.of(a, b, c));

        assertThat(resultado).extracting(ResultadoComparacao::getOpcaoId)
                .containsExactly(b.opcao().getId(), a.opcao().getId(), c.opcao().getId());
        assertThat(resultado).extracting(ResultadoComparacao::getPosicao).containsExactly(1, 2, 3);
        assertThat(resultado).extracting(ResultadoComparacao::getSituacao).containsOnly(SituacaoAlternativa.CLASSIFICADA);
        assertThat(resultado).extracting(ResultadoComparacao::isEmpate).containsOnly(false);
        assertThat(resultado.get(1).getCondicaoPagamento()).isEqualTo("30 dias");
        assertThat(resultado.get(0).getCustoEfetivo()).isEqualByComparingTo("248.03");
    }

    @Test
    void ct20_ct35_empateEDeterministicoPelaOrdemDeCadastroEOPrazoNaoInfluencia() {
        CandidatoComparacao primeiraCadastrada = candidato(opcao(true, "90 dias"), calculado("325.00", "249.11"));
        CandidatoComparacao segundaCadastrada = candidato(opcao(true, "30 dias"), calculado("325.00", "249.11"));
        CandidatoComparacao maisBarata = candidato(opcao(true, null), calculado("210.00", "150.00"));

        // A ordem de entrada não altera o resultado.
        List<ResultadoComparacao> resultado = comparador.classificar(List.of(segundaCadastrada, maisBarata, primeiraCadastrada));
        List<ResultadoComparacao> repetido = comparador.classificar(List.of(primeiraCadastrada, segundaCadastrada, maisBarata));

        assertThat(resultado).extracting(ResultadoComparacao::getOpcaoId)
                .containsExactly(maisBarata.opcao().getId(), primeiraCadastrada.opcao().getId(),
                        segundaCadastrada.opcao().getId());
        assertThat(repetido).extracting(ResultadoComparacao::getOpcaoId)
                .isEqualTo(resultado.stream().map(ResultadoComparacao::getOpcaoId).toList());
        assertThat(resultado).extracting(ResultadoComparacao::isEmpate).containsExactly(false, true, true);
        assertThat(resultado.get(1).getMotivo()).contains("desempate pela ordem de cadastro");
        assertThat(resultado.get(1).getCustoEfetivo()).isEqualByComparingTo(resultado.get(2).getCustoEfetivo());
    }

    @Test
    void empateComparaOValorENaoAEscalaDoNumero() {
        CandidatoComparacao a = candidato(opcao(true, null), calculado("325.00", "249.11"));
        CandidatoComparacao b = candidato(opcao(true, null), calculado("325.00", "249.110"));

        assertThat(comparador.classificar(List.of(a, b))).extracting(ResultadoComparacao::isEmpate)
                .containsExactly(true, true);
    }

    @Test
    void calculoIncompletoNuncaEClassificadoNemTratadoComoCustoZero() {
        CalculoCusto incompleto = calculo(StatusCalculo.INCOMPLETO, "325.00", null, QUANTIDADE, null,
                new PendenciaCalculo("REGRA_AUSENTE", true, "Nenhuma regra ativa de ICMS se aplica."),
                new PendenciaCalculo("CFOPS_PARTICIPANTES_NAO_DEFINIDOS", false, "aviso"));
        CandidatoComparacao semCusto = candidato(opcao(true, null), incompleto);
        CandidatoComparacao valida = candidato(opcao(true, null), calculado("500.00", "400.00"));

        List<ResultadoComparacao> resultado = comparador.classificar(List.of(semCusto, valida));

        assertThat(resultado.get(0).getOpcaoId()).isEqualTo(valida.opcao().getId());
        assertThat(resultado.get(0).getPosicao()).isEqualTo(1);
        ResultadoComparacao naoClassificada = resultado.get(1);
        assertThat(naoClassificada.getSituacao()).isEqualTo(SituacaoAlternativa.CALCULO_INCOMPLETO);
        assertThat(naoClassificada.getPosicao()).isNull();
        assertThat(naoClassificada.getCustoEfetivo()).isNull();
        assertThat(naoClassificada.getStatusCalculo()).isEqualTo(StatusCalculo.INCOMPLETO);
        assertThat(naoClassificada.getCalculoId()).isEqualTo(incompleto.getId());
        // Somente as pendências bloqueantes explicam o motivo.
        assertThat(naoClassificada.getMotivo()).contains("Nenhuma regra ativa de ICMS").doesNotContain("aviso");
    }

    @Test
    void fornecedorEProdutoDesativadosNaoSaoClassificados() {
        CandidatoComparacao fornecedorDesativado = candidato(opcao(false, null), null);
        CandidatoComparacao produtoDesativado = new CandidatoComparacao(opcao(true, null), false, QUANTIDADE, null);
        CandidatoComparacao ambos = new CandidatoComparacao(opcao(false, null), false, QUANTIDADE, null);

        List<ResultadoComparacao> resultado = comparador.classificar(List.of(fornecedorDesativado, produtoDesativado, ambos));

        assertThat(resultado).extracting(ResultadoComparacao::getSituacao).containsExactly(
                SituacaoAlternativa.FORNECEDOR_DESATIVADO, SituacaoAlternativa.PRODUTO_DESATIVADO,
                SituacaoAlternativa.FORNECEDOR_DESATIVADO);
        assertThat(resultado).extracting(ResultadoComparacao::getPosicao).containsOnlyNulls();
        assertThat(resultado.get(0).getMotivo()).contains("O fornecedor está desativado");
        assertThat(resultado.get(0).isFornecedorAtivo()).isFalse();
        assertThat(resultado.get(1).getMotivo()).contains("O produto está desativado");
        assertThat(resultado.get(1).isProdutoAtivo()).isFalse();
        assertThat(resultado.get(2).getMotivo()).contains("O fornecedor está desativado")
                .contains("O produto está desativado");
    }

    @Test
    void cfopForaDosParticipantesNaoEClassificado() {
        CalculoCusto calculo = calculo(StatusCalculo.CALCULADO, "325.00", "249.11", QUANTIDADE, "6910",
                new PendenciaCalculo("CFOP_NAO_PARTICIPANTE", false, "O CFOP 6910 não está em CFOPS_PARTICIPANTES."));

        ResultadoComparacao resultado = comparador.classificar(List.of(candidato(opcao(true, null), calculo))).getFirst();

        assertThat(resultado.getSituacao()).isEqualTo(SituacaoAlternativa.CFOP_NAO_PARTICIPANTE);
        assertThat(resultado.getPosicao()).isNull();
        // O custo calculado continua visível, mas fora do ranking.
        assertThat(resultado.getCustoEfetivo()).isEqualByComparingTo("249.11");
    }

    @Test
    void cfopsParticipantesNaoDefinidoSoImpedeQuandoAOperacaoTemCfop() {
        PendenciaCalculo aviso = new PendenciaCalculo("CFOPS_PARTICIPANTES_NAO_DEFINIDOS", false, "aviso");
        CandidatoComparacao comCfop = candidato(opcao(true, null),
                calculo(StatusCalculo.CALCULADO, "325.00", "249.11", QUANTIDADE, "6102", aviso));
        CandidatoComparacao semCfop = candidato(opcao(true, null),
                calculo(StatusCalculo.CALCULADO, "325.00", "255.00", QUANTIDADE, null, aviso));

        List<ResultadoComparacao> resultado = comparador.classificar(List.of(comCfop, semCfop));

        assertThat(resultado.get(0).getOpcaoId()).isEqualTo(semCfop.opcao().getId());
        assertThat(resultado.get(0).getSituacao()).isEqualTo(SituacaoAlternativa.CLASSIFICADA);
        assertThat(resultado.get(1).getSituacao()).isEqualTo(SituacaoAlternativa.CFOPS_PARTICIPANTES_NAO_DEFINIDOS);
        assertThat(resultado.get(1).getMotivo()).contains("6102").contains("CFOPS_PARTICIPANTES não foi definido");
    }

    @Test
    void quantidadeDiferenteDaCotacaoNaoEClassificada() {
        CandidatoComparacao outraQuantidade = candidato(opcao(true, null),
                calculo(StatusCalculo.CALCULADO, "162.50", "124.56", BigDecimal.ONE, null));
        CandidatoComparacao mesmaQuantidadeOutraEscala = candidato(opcao(true, null),
                calculo(StatusCalculo.CALCULADO, "325.00", "249.11", new BigDecimal("2.0000"), null));

        List<ResultadoComparacao> resultado = comparador.classificar(List.of(outraQuantidade, mesmaQuantidadeOutraEscala));

        assertThat(resultado.get(0).getSituacao()).isEqualTo(SituacaoAlternativa.CLASSIFICADA);
        assertThat(resultado.get(1).getSituacao()).isEqualTo(SituacaoAlternativa.QUANTIDADE_DIVERGENTE);
        assertThat(resultado.get(1).getMotivo()).contains("(1)").contains("(2)").contains("quantidades diferentes");
    }

    @Test
    void naoClassificadasVemDepoisNaOrdemDeCadastro() {
        CandidatoComparacao incompletaA = candidato(opcao(true, null), calculo(StatusCalculo.INCOMPLETO, null, null, null, null));
        CandidatoComparacao valida = candidato(opcao(true, null), calculado("300.00", "250.00"));
        CandidatoComparacao incompletaB = candidato(opcao(true, null), calculo(StatusCalculo.INCOMPLETO, null, null, null, null));

        List<ResultadoComparacao> resultado = comparador.classificar(List.of(incompletaB, valida, incompletaA));

        assertThat(resultado).extracting(ResultadoComparacao::getOpcaoId).containsExactly(
                valida.opcao().getId(), incompletaA.opcao().getId(), incompletaB.opcao().getId());
    }

    @Test
    void semOpcoesClassificaveisORankingFicaVazio() {
        List<ResultadoComparacao> resultado = comparador.classificar(List.of(
                candidato(opcao(true, null), calculo(StatusCalculo.INCOMPLETO, null, null, null, null))));

        assertThat(resultado).hasSize(1);
        assertThat(resultado).extracting(ResultadoComparacao::getPosicao).containsOnlyNulls();
        assertThat(comparador.classificar(List.of())).isEmpty();
    }

    @Test
    void motivoMuitoLongoETruncado() {
        List<PendenciaCalculo> pendencias = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pendencias.add(new PendenciaCalculo("DADO_INDISPONIVEL", true, "x".repeat(400)));
        }
        CalculoCusto calculo = calculo(StatusCalculo.INCOMPLETO, null, null, null, null,
                pendencias.toArray(PendenciaCalculo[]::new));

        String motivo = comparador.classificar(List.of(candidato(opcao(true, null), calculo))).getFirst().getMotivo();

        assertThat(motivo).hasSize(ComparadorAlternativas.TAMANHO_MAXIMO_MOTIVO).endsWith("...");
    }

    // ---- Apoio ----

    private CandidatoComparacao candidato(CotacaoOpcao opcao, CalculoCusto calculo) {
        return new CandidatoComparacao(opcao, true, QUANTIDADE, calculo);
    }

    private CotacaoOpcao opcao(boolean fornecedorAtivo, String condicaoPagamento) {
        long id = ++sequencia;
        Fornecedor fornecedor = new Fornecedor("Fornecedor Teste " + id, "11222333000181", Uf.ES,
                TipoFornecedor.FABRICANTE, "60 dias");
        ReflectionTestUtils.setField(fornecedor, "id", 100 + id);
        fornecedor.setAtivo(fornecedorAtivo);
        CotacaoOpcao opcao = new CotacaoOpcao(fornecedor, null, condicaoPagamento, null);
        ReflectionTestUtils.setField(opcao, "id", id);
        return opcao;
    }

    private CalculoCusto calculado(String valorOperacao, String custoEfetivo) {
        return calculo(StatusCalculo.CALCULADO, valorOperacao, custoEfetivo, QUANTIDADE, null);
    }

    private CalculoCusto calculo(StatusCalculo status, String valorOperacao, String custoEfetivo, BigDecimal quantidade,
                                 String cfop, PendenciaCalculo... pendencias) {
        CalculoCusto calculo = new CalculoCusto(1L, 100L, TipoFornecedor.FABRICANTE);
        ReflectionTestUtils.setField(calculo, "id", 1000 + ++sequencia);
        calculo.definirOperacao(null, null, 20L, Uf.ES, Uf.GO, "0", cfop, quantidade);
        BigDecimal valor = valorOperacao == null ? null : new BigDecimal(valorOperacao);
        BigDecimal custo = custoEfetivo == null ? null : new BigDecimal(custoEfetivo);
        calculo.definirResultado(status, valor, valor, null, custo == null ? null : valor.subtract(custo), null, custo,
                List.of(), List.of(pendencias));
        return calculo;
    }
}
