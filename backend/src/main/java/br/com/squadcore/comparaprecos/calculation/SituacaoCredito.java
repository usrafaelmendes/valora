package br.com.squadcore.comparaprecos.calculation;

/** Situação do crédito de um tributo no cálculo. */
public enum SituacaoCredito {
    /** Crédito calculado pela regra aplicável. */
    CALCULADO,
    /** Regra aplicável declara crédito zero (CT13). */
    SEM_CREDITO,
    /** Nenhuma regra aplicável (CT17): o crédito não é presumido zero. */
    REGRA_AUSENTE,
    /** Regras empatadas: nenhuma é escolhida. */
    CONFLITO_REGRAS,
    /** A alíquota da operação exigida pela regra não está disponível. */
    ALIQUOTA_INDISPONIVEL,
    /** Configuração incoerente para o tributo. */
    CONFIGURACAO_INCONSISTENTE,
    /** Regra e alíquota conhecidas, mas a base dos créditos não pôde ser obtida. */
    NAO_CALCULADO
}
