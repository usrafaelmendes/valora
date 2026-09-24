package br.com.squadcore.comparaprecos.entity;

/**
 * Tipo do fornecedor (RF03). Usado futuramente pelas regras de IPI configuráveis
 * (REGRAS_TRIBUTARIAS §6); nenhuma regra tributária é aplicada aqui.
 */
public enum TipoFornecedor {
    FABRICANTE,
    /** Atacadista ou revendedor. */
    ATACADISTA
}
