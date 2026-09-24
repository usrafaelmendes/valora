package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

/**
 * Crédito de um tributo em um cálculo (tabela calculo_custo_credito). Guarda uma cópia da
 * regra usada (id, versão, nome, forma, alíquota e fator) no momento do cálculo: alterações
 * posteriores da regra não mudam o que foi registrado.
 */
@Embeddable
public class CreditoCalculo {

    @Column(nullable = false, length = 20)
    private String tributo;

    @Column(nullable = false, length = 30)
    private String situacao;

    @Column(name = "regra_id")
    private Long regraId;

    @Column(name = "regra_versao")
    private Integer regraVersao;

    @Column(name = "regra_nome", length = 150)
    private String regraNome;

    @Column(name = "forma_aliquota", length = 20)
    private String formaAliquota;

    @Column(name = "aliquota_obtida", precision = 7, scale = 4)
    private BigDecimal aliquotaObtida;

    @Column(precision = 5, scale = 4)
    private BigDecimal fator;

    @Column(name = "aliquota_aplicada", precision = 12, scale = 8)
    private BigDecimal aliquotaAplicada;

    @Column(name = "base_calculo", precision = 15, scale = 2)
    private BigDecimal baseCalculo;

    @Column(name = "valor_sem_arredondamento", precision = 27, scale = 12)
    private BigDecimal valorSemArredondamento;

    @Column(precision = 15, scale = 2)
    private BigDecimal valor;

    /** Regras empatadas no formato "id:versão", separadas por vírgula. */
    @Column(name = "regras_em_conflito", length = 500)
    private String regrasEmConflito;

    @Column(length = 500)
    private String mensagem;

    protected CreditoCalculo() {
        // exigido pelo JPA
    }

    public CreditoCalculo(String tributo, String situacao, Long regraId, Integer regraVersao, String regraNome,
                          String formaAliquota, BigDecimal aliquotaObtida, BigDecimal fator,
                          BigDecimal aliquotaAplicada, BigDecimal baseCalculo, BigDecimal valorSemArredondamento,
                          BigDecimal valor, String regrasEmConflito, String mensagem) {
        this.tributo = tributo;
        this.situacao = situacao;
        this.regraId = regraId;
        this.regraVersao = regraVersao;
        this.regraNome = regraNome;
        this.formaAliquota = formaAliquota;
        this.aliquotaObtida = aliquotaObtida;
        this.fator = fator;
        this.aliquotaAplicada = aliquotaAplicada;
        this.baseCalculo = baseCalculo;
        this.valorSemArredondamento = valorSemArredondamento;
        this.valor = valor;
        this.regrasEmConflito = regrasEmConflito;
        this.mensagem = mensagem;
    }

    public String getTributo() {
        return tributo;
    }

    public String getSituacao() {
        return situacao;
    }

    public Long getRegraId() {
        return regraId;
    }

    public Integer getRegraVersao() {
        return regraVersao;
    }

    public String getRegraNome() {
        return regraNome;
    }

    public String getFormaAliquota() {
        return formaAliquota;
    }

    public BigDecimal getAliquotaObtida() {
        return aliquotaObtida;
    }

    public BigDecimal getFator() {
        return fator;
    }

    public BigDecimal getAliquotaAplicada() {
        return aliquotaAplicada;
    }

    public BigDecimal getBaseCalculo() {
        return baseCalculo;
    }

    public BigDecimal getValorSemArredondamento() {
        return valorSemArredondamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getRegrasEmConflito() {
        return regrasEmConflito;
    }

    public String getMensagem() {
        return mensagem;
    }
}
