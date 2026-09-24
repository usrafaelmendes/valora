package br.com.squadcore.comparaprecos.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parâmetros gerais do cálculo. O código conhece somente a chave e o formato aceito;
 * o valor é definido pelo ADMIN e fica no banco (tabela parametro_calculo).
 */
public enum ChaveParametro {

    /** UF de destino das operações (REGRAS_TRIBUTARIAS §7). */
    UF_DESTINO("UF de destino das operações.", Formato.VALOR_UNICO,
            Arrays.stream(Uf.values()).map(Enum::name).toList()),

    /**
     * De onde vem a UF de origem: do emitente da NF-e ou do cadastro do fornecedor
     * (REGRAS §7, ARQUITETURA §8 e §18).
     */
    FONTE_UF_ORIGEM("Fonte da UF de origem da operação.", Formato.VALOR_UNICO,
            List.of("EMITENTE_NFE", "CADASTRO_FORNECEDOR")),

    /** Arredondamento por crédito ou somente no total (REGRAS §4, PENDENTE DE VALIDAÇÃO). */
    ARREDONDAMENTO_CREDITOS("Momento do arredondamento dos créditos a 2 casas decimais.", Formato.VALOR_UNICO,
            List.of("POR_CREDITO", "SOMENTE_TOTAL")),

    /** CFOPs de NF-e que participam do cálculo e da comparação, separados por vírgula. */
    CFOPS_PARTICIPANTES("CFOPs de NF-e que participam do cálculo, separados por vírgula.", Formato.LISTA_CFOP,
            List.of()),

    /** De onde vêm os valores (preço) da operação. */
    FONTE_VALORES_OPERACAO("Fonte dos valores da operação: item da NF-e ou valores informados no cálculo.",
            Formato.VALOR_UNICO, List.of("NFE_ITEM", "INFORMADO")),

    /** De onde vêm origem da mercadoria, CFOP e alíquotas destacadas. */
    FONTE_DADOS_FISCAIS("Fonte dos dados fiscais: item da NF-e, dados informados no cálculo "
            + "ou item da última NF-e do mesmo fornecedor e produto.", Formato.VALOR_UNICO,
            List.of("NFE_ITEM", "INFORMADO", "ULTIMA_NFE_FORNECEDOR_PRODUTO")),

    /** Componentes somados no valor da operação, separados por vírgula. */
    COMPOSICAO_VALOR_OPERACAO("Componentes do valor da operação, separados por vírgula (DESCONTO é subtraído).",
            Formato.LISTA_VALORES, componentes()),

    /** Base dos créditos: o próprio valor da operação (REGRAS §4) ou uma lista de componentes. */
    COMPOSICAO_BASE_CREDITOS("Base dos créditos: VALOR_OPERACAO ou componentes separados por vírgula.",
            Formato.LISTA_VALORES,
            Stream.concat(Stream.of(ChaveParametro.VALOR_OPERACAO), componentes().stream()).toList()),

    /** Critério de arredondamento a 2 casas. */
    CRITERIO_ARREDONDAMENTO("Critério de arredondamento a 2 casas: meio para cima (0,125 → 0,13) "
            + "ou meio para o par (0,125 → 0,12).", Formato.VALOR_UNICO,
            List.of("MEIO_PARA_CIMA", "MEIO_PARA_PAR"));

    /** Valor de COMPOSICAO_BASE_CREDITOS que significa "a base é o valor da operação". */
    public static final String VALOR_OPERACAO = "VALOR_OPERACAO";

    private enum Formato { VALOR_UNICO, LISTA_VALORES, LISTA_CFOP }

    private static final Pattern CFOP = Pattern.compile("[0-9]{4}");

    private final String descricao;
    private final Formato formato;
    /** Valores aceitos; vazio para CFOPS_PARTICIPANTES, que aceita uma lista de CFOPs. */
    private final List<String> valoresAceitos;

    ChaveParametro(String descricao, Formato formato, List<String> valoresAceitos) {
        this.descricao = descricao;
        this.formato = formato;
        this.valoresAceitos = valoresAceitos;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<String> getValoresAceitos() {
        return valoresAceitos;
    }

    /**
     * Valida e normaliza o valor informado. Nulo ou em branco volta a "não definido".
     *
     * @throws IllegalArgumentException com a mensagem a ser apresentada ao ADMIN
     */
    public String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return switch (formato) {
            case LISTA_CFOP -> normalizarCfops(valor);
            case LISTA_VALORES -> normalizarLista(valor);
            case VALOR_UNICO -> {
                String normalizado = valor.trim().toUpperCase(Locale.ROOT);
                if (!valoresAceitos.contains(normalizado)) {
                    throw new IllegalArgumentException("Valor inválido. Valores aceitos: "
                            + String.join(", ", valoresAceitos) + ".");
                }
                yield normalizado;
            }
        };
    }

    /** Converte o valor gravado de uma lista de componentes (sem VALOR_OPERACAO). */
    public static List<ComponenteValor> componentesDe(String valor) {
        List<ComponenteValor> componentes = new ArrayList<>();
        for (String parte : valor.split(",")) {
            componentes.add(ComponenteValor.valueOf(parte));
        }
        return componentes;
    }

    /** Sem repetições e na ordem de declaração, para o valor gravado ser sempre o mesmo. */
    private String normalizarLista(String valor) {
        Set<String> escolhidos = new LinkedHashSet<>();
        for (String parte : valor.split(",", -1)) {
            String item = parte.trim().toUpperCase(Locale.ROOT);
            if (!valoresAceitos.contains(item)) {
                throw new IllegalArgumentException("Informe valores separados por vírgula. Valores aceitos: "
                        + String.join(", ", valoresAceitos) + ".");
            }
            escolhidos.add(item);
        }
        if (escolhidos.contains(VALOR_OPERACAO) && escolhidos.size() > 1) {
            throw new IllegalArgumentException(VALOR_OPERACAO + " não pode ser combinado com outros componentes.");
        }
        return escolhidos.stream()
                .sorted(Comparator.comparingInt(valoresAceitos::indexOf))
                .collect(Collectors.joining(","));
    }

    /** Remove espaços e repetições e ordena, para o valor gravado ser sempre o mesmo. */
    private static String normalizarCfops(String valor) {
        TreeSet<String> cfops = new TreeSet<>();
        for (String parte : valor.split(",", -1)) {
            String cfop = parte.trim();
            if (!CFOP.matcher(cfop).matches()) {
                throw new IllegalArgumentException(
                        "Informe CFOPs de 4 dígitos separados por vírgula (ex.: 1234,5678).");
            }
            cfops.add(cfop);
        }
        return String.join(",", cfops);
    }

    private static List<String> componentes() {
        return Arrays.stream(ComponenteValor.values()).map(Enum::name).toList();
    }
}
