package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.Comparacao;
import br.com.squadcore.comparaprecos.entity.Cotacao;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.CreditoCalculo;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.ParametroComparacao;
import br.com.squadcore.comparaprecos.entity.PendenciaCalculo;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.RegraComparacao;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.entity.SituacaoAlternativa;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tabela CSV de uma comparação gravada (RF12, RF13, CT25). Os resultados abaixo são DADOS DE TESTE
 * montados como o ComparacaoService os grava; a tabela só os formata.
 */
class TabelaComparacaoCsvTest {

    private final TabelaComparacaoCsv tabela = new TabelaComparacaoCsv();
    private final Map<Long, CalculoCusto> calculos = new HashMap<>();
    private long sequencia;

    @Test
    void cabecalhoIdentificaAComparacaoEmPortuguesComBomUtf8() {
        Comparacao comparacao = comparacao("Compra de reposição — setembro", List.of());

        byte[] bytes = tabela.gerar(comparacao, "Compra de reposição — setembro", calculos);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
        List<String> linhas = linhas(csv);
        assertThat(linhas.get(0)).isEqualTo("﻿Comparação de fornecedores");
        assertThat(linhas).contains("Comparação;7", "Cotação;3", "Descrição da cotação;Compra de reposição — setembro",
                "Produto;Produto Teste Ação", "Quantidade;2", "Executada em;24/09/2026 03:04:46 (horário de Brasília)",
                "Total de opções;0", "Opções classificadas;0");
        assertThat(csv).contains("Critério de ordenação;Custo efetivo crescente");
        assertThat(csv).endsWith("\r\n");
    }

    @Test
    void mantemAOrdemRegistradaComPosicaoEmpateECreditos() {
        // Ordem registrada propositalmente diferente da ordem das opções: a tabela não reordena.
        ResultadoComparacao primeiro = classificada(fornecedor("Atacadista MG", TipoFornecedor.ATACADISTA, null),
                "45 dias", 1, false, "325.00", "76.97", "248.03");
        ResultadoComparacao segundo = classificada(fornecedor("Fabricante ES", TipoFornecedor.FABRICANTE, "30 dias"),
                "90 dias", 2, true, "325.00", "75.89", "249.11");
        ResultadoComparacao terceiro = classificada(fornecedor("Fabricante ES", TipoFornecedor.FABRICANTE, "30 dias"),
                null, 3, true, "325.00", "75.89", "249.11");
        Comparacao comparacao = comparacao(null, List.of(primeiro, segundo, terceiro));

        List<String> linhas = linhas(csv(comparacao));

        int cabecalho = linhas.indexOf("Posição;Empate;Opção;Fornecedor;Tipo do fornecedor;Condição de pagamento;"
                + "Prazo de pagamento base;Quantidade;Valor da operação (R$);Créditos considerados (R$);"
                + "Total de créditos (R$);Custo efetivo (R$);Observação");
        assertThat(cabecalho).isPositive();
        assertThat(linhas.get(cabecalho + 1)).isEqualTo("1;Não;" + primeiro.getOpcaoId()
                + ";Atacadista MG;Atacadista;45 dias;;2;325,00;ICMS: 22,75 | IPI: 32,50 | PIS/COFINS: 20,64;76,97;248,03;");
        assertThat(linhas.get(cabecalho + 2)).startsWith("2;Sim;" + segundo.getOpcaoId() + ";Fabricante ES;Fabricante;90 dias;30 dias;")
                .endsWith(";75,89;249,11;Mesmo custo efetivo de outra opção: desempate pela ordem de cadastro da opção.");
        assertThat(linhas.get(cabecalho + 3)).startsWith("3;Sim;" + terceiro.getOpcaoId() + ";Fabricante ES;Fabricante;;30 dias;");
        assertThat(linhas).contains("Nenhuma alternativa não classificada.");
    }

    @Test
    void naoClassificadasFicamSeparadasComMotivoESemCustoZero() {
        ResultadoComparacao valida = classificada(fornecedor("Fabricante ES", TipoFornecedor.FABRICANTE, null),
                null, 1, false, "325.00", "75.89", "249.11");
        CalculoCusto incompleto = calculo(StatusCalculo.INCOMPLETO, "325.00", null, null,
                new CreditoCalculo("ICMS", "REGRA_AUSENTE", null, null, null, null, null, null, null, null,
                        null, null, null, "sem regra"));
        ResultadoComparacao semCusto = resultado(fornecedor("Fabricante GO", TipoFornecedor.FABRICANTE, "90 dias"),
                "30 dias", incompleto, SituacaoAlternativa.CALCULO_INCOMPLETO, null, false,
                "O custo efetivo não pôde ser calculado: Nenhuma regra ativa de ICMS se aplica à operação.");
        ResultadoComparacao desativado = resultado(fornecedor("Fornecedor Desativado", TipoFornecedor.ATACADISTA, null),
                null, null, SituacaoAlternativa.FORNECEDOR_DESATIVADO, null, false, "O fornecedor está desativado.");
        Comparacao comparacao = comparacao(null, List.of(valida, semCusto, desativado));

        String csv = csv(comparacao);
        List<String> linhas = linhas(csv);

        int titulo = linhas.indexOf("Alternativas não classificadas (fora do ranking)");
        assertThat(titulo).isGreaterThan(linhas.indexOf("Alternativas classificadas (do menor para o maior custo efetivo)"));
        assertThat(linhas.get(titulo + 1)).startsWith("Opção;Fornecedor;Tipo do fornecedor;Situação;Status do cálculo;");
        assertThat(linhas.get(titulo + 2)).isEqualTo(semCusto.getOpcaoId() + ";Fabricante GO;Fabricante;Cálculo incompleto;"
                + "Incompleto;30 dias;90 dias;2;325,00;ICMS: regra ausente;;;"
                + "O custo efetivo não pôde ser calculado: Nenhuma regra ativa de ICMS se aplica à operação.");
        assertThat(linhas.get(titulo + 3)).isEqualTo(desativado.getOpcaoId() + ";Fornecedor Desativado;Atacadista;"
                + "Fornecedor desativado;Não calculado;;;;;;;;O fornecedor está desativado.");
        // O incompleto nunca aparece como zero, e não entra entre as classificadas.
        assertThat(linhas.get(titulo + 2)).doesNotContain(";0,00;").doesNotContain(";0;");
        assertThat(linhas.subList(0, titulo)).noneMatch(l -> l.contains("Fabricante GO"));
    }

    @Test
    void semAlternativasClassificadasInformaNaTabela() {
        ResultadoComparacao desativado = resultado(fornecedor("Fornecedor X", TipoFornecedor.FABRICANTE, null),
                null, null, SituacaoAlternativa.PRODUTO_DESATIVADO, null, false, "Produto desativado.");

        List<String> linhas = linhas(csv(comparacao(null, List.of(desativado))));

        assertThat(linhas).contains("Nenhuma alternativa classificada.", "Descrição da cotação;");
        assertThat(linhas).anyMatch(l -> l.contains(";Produto desativado;Não calculado;"));
    }

    @Test
    void creditoSemArredondamentoESemValor() {
        CalculoCusto calculo = calculo(StatusCalculo.CALCULADO, "325.00", "75.89", "249.11",
                credito("ICMS", "22.75"),
                new CreditoCalculo("PIS_COFINS", "CALCULADO", 1L, 0, "PIS/COFINS", "PERCENTUAL_FIXO",
                        new BigDecimal("6.35"), BigDecimal.ONE, new BigDecimal("6.35"), new BigDecimal("325.00"),
                        new BigDecimal("20.637500000000"), null, null, null),
                new CreditoCalculo("IPI", "SEM_CREDITO", 2L, 0, "IPI", "SEM_CREDITO", null, null, BigDecimal.ZERO,
                        new BigDecimal("325.00"), BigDecimal.ZERO, new BigDecimal("0.00"), null, null));
        ResultadoComparacao resultado = resultado(fornecedor("F", TipoFornecedor.FABRICANTE, null), null, calculo,
                SituacaoAlternativa.CLASSIFICADA, 1, false, null);

        assertThat(csv(comparacao(null, List.of(resultado))))
                .contains(";ICMS: 22,75 | PIS/COFINS: 20,6375 (sem arredondamento) | IPI: 0,00;");
    }

    @Test
    void escapaSeparadorAspasQuebraDeLinhaEFormulas() {
        ResultadoComparacao resultado = classificada(fornecedor("Comércio \"Alfa\"; Filial", TipoFornecedor.FABRICANTE,
                "=HYPERLINK(\"x\")"), "-30 dias\n(boleto)", 1, false, "325.00", "75.89", "249.11");

        String csv = new String(tabela.gerar(comparacao(null, List.of(resultado)), "@descrição; com separador", calculos),
                StandardCharsets.UTF_8);

        assertThat(csv).contains("Descrição da cotação;\"'@descrição; com separador\"");
        assertThat(csv).contains(";\"Comércio \"\"Alfa\"\"; Filial\";Fabricante;\"'-30 dias\n(boleto)\";"
                + "\"'=HYPERLINK(\"\"x\"\")\";");
    }

    @Test
    void configuracaoUtilizadaComParametrosNaoDefinidosERegras() {
        List<String> linhas = linhas(csv(comparacao(null, List.of())));

        assertThat(linhas).contains("Configuração utilizada em todas as opções", "Parâmetro;Valor",
                "UF_DESTINO;GO", "FONTE_VALORES_OPERACAO;não definido",
                "Regra;Tributo;Versão;Prioridade", "PIS/COFINS - crédito de teste (6,35%);PIS/COFINS;2;0");
    }

    @Test
    void nomeDoArquivo() {
        assertThat(tabela.nomeArquivo(comparacao(null, List.of()))).isEqualTo("comparacao-7-cotacao-3.csv");
    }

    // ---- Apoio ----

    private String csv(Comparacao comparacao) {
        return new String(tabela.gerar(comparacao, null, calculos), StandardCharsets.UTF_8);
    }

    private static List<String> linhas(String csv) {
        return List.of(csv.split("\r\n"));
    }

    private Comparacao comparacao(String descricao, List<ResultadoComparacao> resultados) {
        Produto produto = new Produto("Produto Teste Ação", null, null);
        ReflectionTestUtils.setField(produto, "id", 20L);
        Cotacao cotacao = new Cotacao(produto, new BigDecimal("2.0000"), descricao, 2L);
        ReflectionTestUtils.setField(cotacao, "id", 3L);
        Comparacao comparacao = new Comparacao(cotacao, 2L,
                List.of(new ParametroComparacao("UF_DESTINO", "GO"), new ParametroComparacao("FONTE_VALORES_OPERACAO", null)),
                List.of(new RegraComparacao(100L, 2, "PIS/COFINS - crédito de teste (6,35%)", "PIS_COFINS", 0)),
                resultados);
        ReflectionTestUtils.setField(comparacao, "id", 7L);
        ReflectionTestUtils.setField(comparacao, "executadoEm", Instant.parse("2026-09-24T06:04:46Z"));
        return comparacao;
    }

    private ResultadoComparacao classificada(Fornecedor fornecedor, String condicao, int posicao, boolean empate,
                                             String valor, String creditos, String custo) {
        CalculoCusto calculo = calculo(StatusCalculo.CALCULADO, valor, creditos, custo,
                credito("ICMS", "22.75"), credito("IPI", "32.50"), credito("PIS_COFINS", "20.64"));
        return resultado(fornecedor, condicao, calculo, SituacaoAlternativa.CLASSIFICADA, posicao, empate,
                empate ? "Mesmo custo efetivo de outra opção: desempate pela ordem de cadastro da opção." : null);
    }

    private ResultadoComparacao resultado(Fornecedor fornecedor, String condicao, CalculoCusto calculo,
                                          SituacaoAlternativa situacao, Integer posicao, boolean empate, String motivo) {
        CotacaoOpcao opcao = new CotacaoOpcao(fornecedor, null, condicao, null);
        ReflectionTestUtils.setField(opcao, "id", 50 + ++sequencia);
        return new ResultadoComparacao(opcao, true, calculo, situacao, posicao, empate, motivo);
    }

    private Fornecedor fornecedor(String nome, TipoFornecedor tipo, String prazo) {
        Fornecedor fornecedor = new Fornecedor(nome, "11222333000181", Uf.ES, tipo, prazo);
        ReflectionTestUtils.setField(fornecedor, "id", 10 + ++sequencia);
        if (nome.contains("Desativado")) {
            fornecedor.setAtivo(false);
        }
        return fornecedor;
    }

    private CalculoCusto calculo(StatusCalculo status, String valor, String creditos, String custo,
                                 CreditoCalculo... creditosDoCalculo) {
        CalculoCusto calculo = new CalculoCusto(2L, 10L, TipoFornecedor.FABRICANTE);
        long id = 900 + ++sequencia;
        ReflectionTestUtils.setField(calculo, "id", id);
        calculo.definirOperacao(null, null, 20L, Uf.ES, Uf.GO, "0", null, new BigDecimal("2.0000"));
        calculo.definirResultado(status, new BigDecimal(valor), new BigDecimal(valor), null,
                creditos == null ? null : new BigDecimal(creditos), null, custo == null ? null : new BigDecimal(custo),
                List.of(creditosDoCalculo),
                status == StatusCalculo.INCOMPLETO ? List.of(new PendenciaCalculo("REGRA_AUSENTE", true, "sem regra")) : List.of());
        calculos.put(id, calculo);
        return calculo;
    }

    private static CreditoCalculo credito(String tributo, String valor) {
        return new CreditoCalculo(tributo, "CALCULADO", 1L, 0, tributo, "ALIQUOTA_DA_NFE", null, BigDecimal.ONE, null,
                null, new BigDecimal(valor), new BigDecimal(valor), null, null);
    }
}
