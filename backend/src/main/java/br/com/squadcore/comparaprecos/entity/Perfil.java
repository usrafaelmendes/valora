package br.com.squadcore.comparaprecos.entity;

/**
 * Perfis de acesso do sistema (RF02).
 */
public enum Perfil {
    ADMIN,
    /** Não altera cadastros nem regras; consulta dados e realiza comparações. */
    USER
}
