package br.com.squadcore.comparaprecos.entity;

/**
 * Componentes de valor que podem compor o "valor da operação" e a base dos créditos
 * (parâmetros COMPOSICAO_VALOR_OPERACAO e COMPOSICAO_BASE_CREDITOS). Correspondem aos
 * campos do item da NF-e (grupo prod e IPI) ou aos valores informados no cálculo.
 */
public enum ComponenteValor {
    /** vProd: valor dos produtos do item. */
    VALOR_PRODUTO,
    /** vIPI do item. */
    IPI,
    /** vFrete do item. */
    FRETE,
    /** vSeg do item. */
    SEGURO,
    /** vOutro do item. */
    OUTRAS_DESPESAS,
    /** vDesc do item: único componente subtraído. */
    DESCONTO;

    public boolean subtrai() {
        return this == DESCONTO;
    }
}
