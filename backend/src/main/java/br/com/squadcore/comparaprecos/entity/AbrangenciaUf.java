package br.com.squadcore.comparaprecos.entity;

/** Condição sobre as UFs de origem e de destino da operação (REGRAS_TRIBUTARIAS §7). */
public enum AbrangenciaUf {
    /** Origem e destino na mesma UF. */
    INTERNA,
    /** Origem em UF diferente do destino. */
    INTERESTADUAL
}
