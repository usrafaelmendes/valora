package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Opção de fornecedor de uma cotação (tabela cotacao_opcao, migration V8).
 *
 * Guarda o que foi informado: fornecedor, item de NF-e de referência (opcional), condição de
 * pagamento, valores e dados fiscais. Quais desses dados entram no cálculo é decidido pelos
 * parâmetros FONTE_VALORES_OPERACAO e FONTE_DADOS_FISCAIS no momento da comparação.
 * Campo nulo = não informado (nunca é tratado como zero).
 */
@Entity
@Table(name = "cotacao_opcao")
public class CotacaoOpcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotacao_id", nullable = false, updatable = false)
    private Cotacao cotacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false, updatable = false)
    private Fornecedor fornecedor;

    @Column(name = "nfe_item_id", updatable = false)
    private Long nfeItemId;

    @Column(name = "condicao_pagamento", length = 100, updatable = false)
    private String condicaoPagamento;

    @Column(length = 500, updatable = false)
    private String observacao;

    // ---- Valores informados ----

    @Column(name = "valor_produto", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorProduto;

    @Column(name = "valor_ipi", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorIpi;

    @Column(name = "valor_frete", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorFrete;

    @Column(name = "valor_seguro", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorSeguro;

    @Column(name = "valor_outras_despesas", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorOutrasDespesas;

    @Column(name = "valor_desconto", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorDesconto;

    // ---- Dados fiscais informados ----

    @Column(name = "origem_mercadoria", length = 1, updatable = false)
    private String origemMercadoria;

    @Column(length = 4, updatable = false)
    private String cfop;

    @Column(name = "aliquota_icms", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaIcms;

    @Column(name = "aliquota_ipi", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaIpi;

    @Column(name = "aliquota_pis", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaPis;

    @Column(name = "aliquota_cofins", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaCofins;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected CotacaoOpcao() {
        // exigido pelo JPA
    }

    public CotacaoOpcao(Fornecedor fornecedor, Long nfeItemId, String condicaoPagamento, String observacao) {
        this.fornecedor = fornecedor;
        this.nfeItemId = nfeItemId;
        this.condicaoPagamento = condicaoPagamento;
        this.observacao = observacao;
    }

    void associarCotacao(Cotacao cotacao) {
        this.cotacao = cotacao;
    }

    public void definirValores(BigDecimal valorProduto, BigDecimal valorIpi, BigDecimal valorFrete,
                               BigDecimal valorSeguro, BigDecimal valorOutrasDespesas, BigDecimal valorDesconto) {
        this.valorProduto = valorProduto;
        this.valorIpi = valorIpi;
        this.valorFrete = valorFrete;
        this.valorSeguro = valorSeguro;
        this.valorOutrasDespesas = valorOutrasDespesas;
        this.valorDesconto = valorDesconto;
    }

    public void definirDadosFiscais(String origemMercadoria, String cfop, BigDecimal aliquotaIcms,
                                    BigDecimal aliquotaIpi, BigDecimal aliquotaPis, BigDecimal aliquotaCofins) {
        this.origemMercadoria = origemMercadoria;
        this.cfop = cfop;
        this.aliquotaIcms = aliquotaIcms;
        this.aliquotaIpi = aliquotaIpi;
        this.aliquotaPis = aliquotaPis;
        this.aliquotaCofins = aliquotaCofins;
    }

    public boolean temValores() {
        return valorProduto != null || valorIpi != null || valorFrete != null || valorSeguro != null
                || valorOutrasDespesas != null || valorDesconto != null;
    }

    public boolean temDadosFiscais() {
        return origemMercadoria != null || cfop != null || aliquotaIcms != null || aliquotaIpi != null
                || aliquotaPis != null || aliquotaCofins != null;
    }

    public Long getId() {
        return id;
    }

    public Cotacao getCotacao() {
        return cotacao;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public Long getNfeItemId() {
        return nfeItemId;
    }

    public String getCondicaoPagamento() {
        return condicaoPagamento;
    }

    public String getObservacao() {
        return observacao;
    }

    public BigDecimal getValorProduto() {
        return valorProduto;
    }

    public BigDecimal getValorIpi() {
        return valorIpi;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public BigDecimal getValorSeguro() {
        return valorSeguro;
    }

    public BigDecimal getValorOutrasDespesas() {
        return valorOutrasDespesas;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public String getOrigemMercadoria() {
        return origemMercadoria;
    }

    public String getCfop() {
        return cfop;
    }

    public BigDecimal getAliquotaIcms() {
        return aliquotaIcms;
    }

    public BigDecimal getAliquotaIpi() {
        return aliquotaIpi;
    }

    public BigDecimal getAliquotaPis() {
        return aliquotaPis;
    }

    public BigDecimal getAliquotaCofins() {
        return aliquotaCofins;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
