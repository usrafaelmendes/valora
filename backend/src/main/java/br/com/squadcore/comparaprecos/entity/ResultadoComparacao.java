package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * Resultado de uma opção em uma comparação (tabela comparacao_resultado). Guarda a situação,
 * a posição (somente CLASSIFICADA), o cálculo executado e uma cópia dos dados apresentados,
 * para que o resultado continue explicável depois de alterações nos cadastros.
 */
@Embeddable
public class ResultadoComparacao {

    @Column(name = "opcao_id", nullable = false)
    private Long opcaoId;

    @Column(name = "calculo_id")
    private Long calculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SituacaoAlternativa situacao;

    private Integer posicao;

    @Column(nullable = false)
    private boolean empate;

    @Column(length = 2000)
    private String motivo;

    @Column(name = "fornecedor_id", nullable = false)
    private Long fornecedorId;

    @Column(name = "fornecedor_razao_social", nullable = false, length = 150)
    private String fornecedorRazaoSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fornecedor", nullable = false, length = 20)
    private TipoFornecedor tipoFornecedor;

    @Column(name = "fornecedor_ativo", nullable = false)
    private boolean fornecedorAtivo;

    @Column(name = "produto_ativo", nullable = false)
    private boolean produtoAtivo;

    @Column(name = "condicao_pagamento", length = 100)
    private String condicaoPagamento;

    @Column(name = "prazo_pagamento_base", length = 100)
    private String prazoPagamentoBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_calculo", length = 20)
    private StatusCalculo statusCalculo;

    @Column(precision = 15, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "valor_operacao", precision = 15, scale = 2)
    private BigDecimal valorOperacao;

    @Column(name = "total_creditos", precision = 15, scale = 2)
    private BigDecimal totalCreditos;

    @Column(name = "custo_efetivo", precision = 15, scale = 2)
    private BigDecimal custoEfetivo;

    protected ResultadoComparacao() {
        // exigido pelo JPA
    }

    /**
     * @param calculo cálculo executado para a opção; nulo quando ela não pôde ser calculada
     */
    public ResultadoComparacao(CotacaoOpcao opcao, boolean produtoAtivo, CalculoCusto calculo,
                               SituacaoAlternativa situacao, Integer posicao, boolean empate, String motivo) {
        Fornecedor fornecedor = opcao.getFornecedor();
        this.opcaoId = opcao.getId();
        this.fornecedorId = fornecedor.getId();
        this.fornecedorRazaoSocial = fornecedor.getRazaoSocial();
        this.tipoFornecedor = fornecedor.getTipo();
        this.fornecedorAtivo = fornecedor.isAtivo();
        this.prazoPagamentoBase = fornecedor.getPrazoPagamentoBase();
        this.condicaoPagamento = opcao.getCondicaoPagamento();
        this.produtoAtivo = produtoAtivo;
        this.situacao = situacao;
        this.posicao = posicao;
        this.empate = empate;
        this.motivo = motivo;
        if (calculo != null) {
            this.calculoId = calculo.getId();
            this.statusCalculo = calculo.getStatus();
            this.quantidade = calculo.getQuantidade();
            this.valorOperacao = calculo.getValorOperacao();
            this.totalCreditos = calculo.getTotalCreditos();
            this.custoEfetivo = calculo.getCustoEfetivo();
        }
    }

    public Long getOpcaoId() {
        return opcaoId;
    }

    public Long getCalculoId() {
        return calculoId;
    }

    public SituacaoAlternativa getSituacao() {
        return situacao;
    }

    public Integer getPosicao() {
        return posicao;
    }

    public boolean isEmpate() {
        return empate;
    }

    public String getMotivo() {
        return motivo;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public String getFornecedorRazaoSocial() {
        return fornecedorRazaoSocial;
    }

    public TipoFornecedor getTipoFornecedor() {
        return tipoFornecedor;
    }

    public boolean isFornecedorAtivo() {
        return fornecedorAtivo;
    }

    public boolean isProdutoAtivo() {
        return produtoAtivo;
    }

    public String getCondicaoPagamento() {
        return condicaoPagamento;
    }

    public String getPrazoPagamentoBase() {
        return prazoPagamentoBase;
    }

    public StatusCalculo getStatusCalculo() {
        return statusCalculo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorOperacao() {
        return valorOperacao;
    }

    public BigDecimal getTotalCreditos() {
        return totalCreditos;
    }

    public BigDecimal getCustoEfetivo() {
        return custoEfetivo;
    }
}
