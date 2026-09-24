package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.calculation.ComparadorAlternativas;
import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.Comparacao;
import br.com.squadcore.comparaprecos.entity.CreditoCalculo;
import br.com.squadcore.comparaprecos.entity.ParametroComparacao;
import br.com.squadcore.comparaprecos.entity.RegraComparacao;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.entity.SituacaoAlternativa;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tabela de uma comparação já executada em CSV (RF12, RF13, CT25).
 *
 * Usa somente o que foi gravado: o resultado de cada opção (na ordem registrada), os cálculos
 * (imutáveis) e a cópia da configuração. Nada é recalculado nem reordenado, e valor ausente fica
 * em branco (um cálculo INCOMPLETO nunca aparece como custo zero).
 *
 * Formato (decisão técnica, RF13/ARQUITETURA §18): padrão das planilhas em português, com ";"
 * como separador, vírgula decimal, UTF-8 com BOM e quebra de linha CRLF, para abrir diretamente
 * no Excel ou no LibreOffice.
 */
@Component
public class TabelaComparacaoCsv {

    static final String SEPARADOR = ";";
    private static final String BOM = "﻿";
    private static final String FIM_DE_LINHA = "\r\n";
    /** Horário apresentado ao usuário; o banco guarda o instante (TIMESTAMPTZ). */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(FUSO);

    /**
     * @param descricaoCotacao descrição da cotação (nula quando não informada)
     * @param calculos         cálculos da comparação, por id
     */
    public byte[] gerar(Comparacao comparacao, String descricaoCotacao, Map<Long, CalculoCusto> calculos) {
        List<List<String>> linhas = new ArrayList<>();
        linhas.add(List.of("Comparação de fornecedores"));
        linhas.add(List.of("Comparação", String.valueOf(comparacao.getId())));
        linhas.add(List.of("Cotação", String.valueOf(comparacao.getCotacaoId())));
        linhas.add(List.of("Descrição da cotação", texto(descricaoCotacao)));
        linhas.add(List.of("Produto", texto(comparacao.getProdutoNome())));
        linhas.add(List.of("Quantidade", quantidade(comparacao.getQuantidade())));
        linhas.add(List.of("Executada em", comparacao.getExecutadoEm() == null ? ""
                : DATA_HORA.format(comparacao.getExecutadoEm()) + " (horário de Brasília)"));
        linhas.add(List.of("Critério de ordenação", ComparadorAlternativas.CRITERIO));
        linhas.add(List.of("Total de opções", String.valueOf(comparacao.getTotalOpcoes())));
        linhas.add(List.of("Opções classificadas", String.valueOf(comparacao.getTotalClassificadas())));

        List<ResultadoComparacao> classificadas = comparacao.getResultados().stream()
                .filter(r -> r.getSituacao() == SituacaoAlternativa.CLASSIFICADA).toList();
        List<ResultadoComparacao> naoClassificadas = comparacao.getResultados().stream()
                .filter(r -> r.getSituacao() != SituacaoAlternativa.CLASSIFICADA).toList();

        linhas.add(List.of());
        linhas.add(List.of("Alternativas classificadas (do menor para o maior custo efetivo)"));
        if (classificadas.isEmpty()) {
            linhas.add(List.of("Nenhuma alternativa classificada."));
        } else {
            linhas.add(List.of("Posição", "Empate", "Opção", "Fornecedor", "Tipo do fornecedor",
                    "Condição de pagamento", "Prazo de pagamento base", "Quantidade", "Valor da operação (R$)",
                    "Créditos considerados (R$)", "Total de créditos (R$)", "Custo efetivo (R$)", "Observação"));
            for (ResultadoComparacao r : classificadas) {
                linhas.add(List.of(String.valueOf(r.getPosicao()), r.isEmpate() ? "Sim" : "Não",
                        String.valueOf(r.getOpcaoId()), texto(r.getFornecedorRazaoSocial()), tipo(r.getTipoFornecedor()),
                        texto(r.getCondicaoPagamento()), texto(r.getPrazoPagamentoBase()), quantidade(r.getQuantidade()),
                        moeda(r.getValorOperacao()), creditos(calculos.get(r.getCalculoId())),
                        moeda(r.getTotalCreditos()), moeda(r.getCustoEfetivo()), texto(r.getMotivo())));
            }
        }

        linhas.add(List.of());
        linhas.add(List.of("Alternativas não classificadas (fora do ranking)"));
        if (naoClassificadas.isEmpty()) {
            linhas.add(List.of("Nenhuma alternativa não classificada."));
        } else {
            linhas.add(List.of("Opção", "Fornecedor", "Tipo do fornecedor", "Situação", "Status do cálculo",
                    "Condição de pagamento", "Prazo de pagamento base", "Quantidade", "Valor da operação (R$)",
                    "Créditos considerados (R$)", "Total de créditos (R$)", "Custo efetivo (R$)", "Motivo"));
            for (ResultadoComparacao r : naoClassificadas) {
                linhas.add(List.of(String.valueOf(r.getOpcaoId()), texto(r.getFornecedorRazaoSocial()),
                        tipo(r.getTipoFornecedor()), situacao(r.getSituacao()), status(r.getStatusCalculo()),
                        texto(r.getCondicaoPagamento()), texto(r.getPrazoPagamentoBase()), quantidade(r.getQuantidade()),
                        moeda(r.getValorOperacao()), creditos(calculos.get(r.getCalculoId())),
                        moeda(r.getTotalCreditos()), moeda(r.getCustoEfetivo()), texto(r.getMotivo())));
            }
        }

        linhas.add(List.of());
        linhas.add(List.of("Configuração utilizada em todas as opções"));
        linhas.add(List.of("Parâmetro", "Valor"));
        for (ParametroComparacao p : comparacao.getParametros()) {
            linhas.add(List.of(p.getChave(), p.getValor() == null ? "não definido" : texto(p.getValor())));
        }
        linhas.add(List.of());
        linhas.add(List.of("Regras tributárias ativas"));
        if (comparacao.getRegras().isEmpty()) {
            linhas.add(List.of("Nenhuma regra ativa."));
        } else {
            linhas.add(List.of("Regra", "Tributo", "Versão", "Prioridade"));
            for (RegraComparacao r : comparacao.getRegras()) {
                linhas.add(List.of(texto(r.getRegraNome()), tributo(r.getTributo()), String.valueOf(r.getRegraVersao()),
                        String.valueOf(r.getPrioridade())));
            }
        }

        String conteudo = linhas.stream()
                .map(linha -> linha.stream().map(TabelaComparacaoCsv::celula).collect(Collectors.joining(SEPARADOR)))
                .collect(Collectors.joining(FIM_DE_LINHA, BOM, FIM_DE_LINHA));
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    public String nomeArquivo(Comparacao comparacao) {
        return "comparacao-" + comparacao.getId() + "-cotacao-" + comparacao.getCotacaoId() + ".csv";
    }

    /**
     * Crédito de cada tributo como foi gravado no cálculo. Sem valor arredondado (arredondamento
     * só no total), mostra o valor sem arredondamento; sem valor, mostra a situação do crédito.
     */
    private static String creditos(CalculoCusto calculo) {
        if (calculo == null) {
            return "";
        }
        return calculo.getCreditos().stream()
                .map(c -> tributo(c.getTributo()) + ": " + valorDoCredito(c))
                .collect(Collectors.joining(" | "));
    }

    private static String valorDoCredito(CreditoCalculo c) {
        if (c.getValor() != null) {
            return moeda(c.getValor());
        }
        if (c.getValorSemArredondamento() != null) {
            return c.getValorSemArredondamento().stripTrailingZeros().toPlainString().replace('.', ',')
                    + " (sem arredondamento)";
        }
        return c.getSituacao().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String moeda(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }

    private static String quantidade(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String tributo(String tributo) {
        return "PIS_COFINS".equals(tributo) ? "PIS/COFINS" : tributo;
    }

    private static String tipo(TipoFornecedor tipo) {
        return tipo == TipoFornecedor.FABRICANTE ? "Fabricante" : "Atacadista";
    }

    private static String status(StatusCalculo status) {
        if (status == null) {
            return "Não calculado";
        }
        return status == StatusCalculo.CALCULADO ? "Calculado" : "Incompleto";
    }

    private static String situacao(SituacaoAlternativa situacao) {
        return switch (situacao) {
            case CLASSIFICADA -> "Classificada";
            case CALCULO_INCOMPLETO -> "Cálculo incompleto";
            case FORNECEDOR_DESATIVADO -> "Fornecedor desativado";
            case PRODUTO_DESATIVADO -> "Produto desativado";
            case CFOP_NAO_PARTICIPANTE -> "CFOP não participante";
            case CFOPS_PARTICIPANTES_NAO_DEFINIDOS -> "CFOPs participantes não definidos";
            case QUANTIDADE_DIVERGENTE -> "Quantidade divergente";
        };
    }

    /**
     * Texto livre informado por usuários (descrição, condição, razão social). Um valor que começa
     * com =, +, - ou @ seria interpretado como fórmula pela planilha: recebe um apóstrofo antes.
     */
    static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return !valor.isEmpty() && "=+-@\t\r".indexOf(valor.charAt(0)) >= 0 ? "'" + valor : valor;
    }

    /** Aspas quando a célula tem separador, aspas ou quebra de linha (RFC 4180). */
    private static String celula(String valor) {
        boolean precisaAspas = Arrays.stream(new String[]{SEPARADOR, "\"", "\n", "\r"}).anyMatch(valor::contains);
        return precisaAspas ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
    }
}
