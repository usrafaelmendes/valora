package br.com.squadcore.comparaprecos.entity;

/** Como a regra obtém a alíquota do crédito. */
public enum FormaAliquota {
    /** Percentual configurado na própria regra. */
    PERCENTUAL_FIXO,
    /** Alíquota do tributo destacada no item da NF-e (pICMS, pIPI, pPIS, pCOFINS). */
    ALIQUOTA_DA_NFE,
    /** Crédito zero declarado explicitamente (CT13); diferente de não haver regra (CT17). */
    SEM_CREDITO
}
