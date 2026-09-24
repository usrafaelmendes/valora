package br.com.squadcore.comparaprecos.entity;

/**
 * Situação de uma opção em uma comparação. Somente CLASSIFICADA entra no ranking; as demais
 * são apresentadas separadamente, com o motivo (um cálculo INCOMPLETO nunca vira custo zero).
 */
public enum SituacaoAlternativa {
    /** Cálculo CALCULADO e participação confirmada: tem posição no ranking. */
    CLASSIFICADA,
    /** O cálculo tem pendência bloqueante (parâmetro, regra, dado ausente ou conflito). */
    CALCULO_INCOMPLETO,
    /** Fornecedor desativado: a opção não é calculada nem classificada. */
    FORNECEDOR_DESATIVADO,
    /** Produto desativado: a opção não é calculada nem classificada. */
    PRODUTO_DESATIVADO,
    /** O CFOP da operação não está em CFOPS_PARTICIPANTES. */
    CFOP_NAO_PARTICIPANTE,
    /** A operação tem CFOP, mas CFOPS_PARTICIPANTES não foi definido. */
    CFOPS_PARTICIPANTES_NAO_DEFINIDOS,
    /** A quantidade da operação calculada difere da quantidade da cotação. */
    QUANTIDADE_DIVERGENTE
}
