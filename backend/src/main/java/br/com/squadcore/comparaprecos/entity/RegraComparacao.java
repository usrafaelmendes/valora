package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Regra ativa no momento da comparação, com a versão vigente (tabela comparacao_regra). */
@Embeddable
public class RegraComparacao {

    @Column(name = "regra_id", nullable = false)
    private Long regraId;

    @Column(name = "regra_versao", nullable = false)
    private int regraVersao;

    @Column(name = "regra_nome", nullable = false, length = 150)
    private String regraNome;

    @Column(nullable = false, length = 20)
    private String tributo;

    @Column(nullable = false)
    private int prioridade;

    protected RegraComparacao() {
        // exigido pelo JPA
    }

    public RegraComparacao(Long regraId, int regraVersao, String regraNome, String tributo, int prioridade) {
        this.regraId = regraId;
        this.regraVersao = regraVersao;
        this.regraNome = regraNome;
        this.tributo = tributo;
        this.prioridade = prioridade;
    }

    public Long getRegraId() {
        return regraId;
    }

    public int getRegraVersao() {
        return regraVersao;
    }

    public String getRegraNome() {
        return regraNome;
    }

    public String getTributo() {
        return tributo;
    }

    public int getPrioridade() {
        return prioridade;
    }
}
