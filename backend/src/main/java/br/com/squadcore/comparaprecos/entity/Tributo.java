package br.com.squadcore.comparaprecos.entity;

/**
 * Tributo ao qual a regra de crédito se refere (REGRAS_TRIBUTARIAS §3).
 *
 * PIS_COFINS representa um crédito combinado de PIS e COFINS (REGRAS §4);
 * PIS e COFINS permitem configurar os créditos separadamente (REGRAS §10). Uma regra
 * PIS_COFINS cobre os dois tributos na seleção de regras.
 */
public enum Tributo {
    ICMS,
    IPI,
    PIS,
    COFINS,
    PIS_COFINS
}
