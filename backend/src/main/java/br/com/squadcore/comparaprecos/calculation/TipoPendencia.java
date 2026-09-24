package br.com.squadcore.comparaprecos.calculation;

/**
 * Situações registradas no cálculo. As bloqueantes impedem o custo efetivo (status
 * INCOMPLETO): o sistema não assume crédito, valor ou parâmetro que não foi definido.
 * Os avisos não impedem o cálculo, mas ficam registrados para explicar o resultado.
 */
public enum TipoPendencia {
    /** Parâmetro de cálculo necessário ainda não definido pelo ADMIN. */
    PARAMETRO_NAO_DEFINIDO(true),
    /** A fonte configurada não tem o dado necessário (ex.: sem item de NF-e, valor não informado). */
    DADO_INDISPONIVEL(true),
    /** Valor obtido que não pode ser usado (ex.: valor da operação negativo). */
    VALOR_INVALIDO(true),
    /** Nenhuma regra ativa se aplica ao tributo (CT17). */
    REGRA_AUSENTE(true),
    /** Mais de uma regra ativa com a mesma maior prioridade. */
    CONFLITO_REGRAS(true),
    /** A regra usa a alíquota da operação, mas ela não existe nos dados fiscais. */
    ALIQUOTA_INDISPONIVEL(true),
    /** Configuração que levaria a crédito duplicado ou incoerente (ex.: PIS/COFINS combinado aplicado a um só dos tributos). */
    CONFIGURACAO_INCONSISTENTE(true),

    /** Componente sem tag no item da NF-e: considerado zero (a tag só existe quando há valor). */
    COMPONENTE_AUSENTE_NA_NFE(false),
    /** O CFOP da operação não está em CFOPS_PARTICIPANTES. */
    CFOP_NAO_PARTICIPANTE(false),
    /** CFOPS_PARTICIPANTES não definido: participação na comparação não verificada. */
    CFOPS_PARTICIPANTES_NAO_DEFINIDOS(false),
    /** O fornecedor está desativado. */
    FORNECEDOR_DESATIVADO(false),
    /** O item da NF-e não está vinculado a um produto cadastrado. */
    PRODUTO_NAO_VINCULADO(false),
    /** Os dados fiscais vieram de outro item de NF-e (ULTIMA_NFE_FORNECEDOR_PRODUTO). */
    DADOS_FISCAIS_DE_OUTRA_NFE(false);

    private final boolean bloqueante;

    TipoPendencia(boolean bloqueante) {
        this.bloqueante = bloqueante;
    }

    public boolean isBloqueante() {
        return bloqueante;
    }
}
