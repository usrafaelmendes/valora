package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cálculo de custo efetivo executado (tabela calculo_custo, migration V7).
 *
 * É um registro histórico e imutável: guarda uma cópia da operação, dos parâmetros, dos
 * valores, das alíquotas e das regras usadas, para explicar o resultado mesmo depois de
 * alterações nas regras ou nos parâmetros. As referências a fornecedor, produto, item de
 * NF-e e usuário ficam como ids (com chave estrangeira no banco).
 */
@Entity
@Table(name = "calculo_custo")
public class CalculoCusto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private StatusCalculo status;

    @CreationTimestamp
    @Column(name = "executado_em", nullable = false, updatable = false)
    private Instant executadoEm;

    @Column(name = "executado_por_id", nullable = false, updatable = false)
    private Long executadoPorId;

    // ---- Operação ----

    @Column(name = "nfe_item_id", updatable = false)
    private Long nfeItemId;

    @Column(name = "nfe_item_dados_fiscais_id", updatable = false)
    private Long nfeItemDadosFiscaisId;

    @Column(name = "fornecedor_id", nullable = false, updatable = false)
    private Long fornecedorId;

    @Column(name = "produto_id", updatable = false)
    private Long produtoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fornecedor", nullable = false, length = 20, updatable = false)
    private TipoFornecedor tipoFornecedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "uf_origem", length = 2, updatable = false)
    private Uf ufOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "uf_destino", length = 2, updatable = false)
    private Uf ufDestino;

    @Column(name = "origem_mercadoria", length = 1, updatable = false)
    private String origemMercadoria;

    @Column(length = 4, updatable = false)
    private String cfop;

    @Column(precision = 15, scale = 4, updatable = false)
    private BigDecimal quantidade;

    // ---- Parâmetros vigentes ----

    @Column(name = "param_fonte_valores", length = 40, updatable = false)
    private String paramFonteValores;

    @Column(name = "param_fonte_dados_fiscais", length = 40, updatable = false)
    private String paramFonteDadosFiscais;

    @Column(name = "param_fonte_uf_origem", length = 40, updatable = false)
    private String paramFonteUfOrigem;

    @Column(name = "param_composicao_valor_operacao", length = 200, updatable = false)
    private String paramComposicaoValorOperacao;

    @Column(name = "param_composicao_base_creditos", length = 200, updatable = false)
    private String paramComposicaoBaseCreditos;

    @Column(name = "param_arredondamento", length = 40, updatable = false)
    private String paramArredondamento;

    @Column(name = "param_criterio_arredondamento", length = 40, updatable = false)
    private String paramCriterioArredondamento;

    // ---- Componentes de valor usados ----

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

    // ---- Alíquotas dos dados fiscais da operação ----

    @Column(name = "aliquota_icms_operacao", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaIcmsOperacao;

    @Column(name = "aliquota_ipi_operacao", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaIpiOperacao;

    @Column(name = "aliquota_pis_operacao", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaPisOperacao;

    @Column(name = "aliquota_cofins_operacao", precision = 7, scale = 4, updatable = false)
    private BigDecimal aliquotaCofinsOperacao;

    // ---- Resultado ----

    @Column(name = "valor_operacao", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorOperacao;

    @Column(name = "base_creditos", precision = 15, scale = 2, updatable = false)
    private BigDecimal baseCreditos;

    @Column(name = "total_creditos_sem_arredondamento", precision = 27, scale = 12, updatable = false)
    private BigDecimal totalCreditosSemArredondamento;

    @Column(name = "total_creditos", precision = 15, scale = 2, updatable = false)
    private BigDecimal totalCreditos;

    @Column(name = "diferenca_arredondamento", precision = 27, scale = 12, updatable = false)
    private BigDecimal diferencaArredondamento;

    @Column(name = "custo_efetivo", precision = 15, scale = 2, updatable = false)
    private BigDecimal custoEfetivo;

    @ElementCollection
    @CollectionTable(name = "calculo_custo_credito", joinColumns = @JoinColumn(name = "calculo_id"))
    @OrderColumn(name = "posicao")
    private List<CreditoCalculo> creditos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "calculo_custo_pendencia", joinColumns = @JoinColumn(name = "calculo_id"))
    @OrderColumn(name = "posicao")
    private List<PendenciaCalculo> pendencias = new ArrayList<>();

    protected CalculoCusto() {
        // exigido pelo JPA
    }

    public CalculoCusto(Long executadoPorId, Long fornecedorId, TipoFornecedor tipoFornecedor) {
        this.executadoPorId = executadoPorId;
        this.fornecedorId = fornecedorId;
        this.tipoFornecedor = tipoFornecedor;
    }

    public void definirOperacao(Long nfeItemId, Long nfeItemDadosFiscaisId, Long produtoId, Uf ufOrigem,
                                Uf ufDestino, String origemMercadoria, String cfop, BigDecimal quantidade) {
        this.nfeItemId = nfeItemId;
        this.nfeItemDadosFiscaisId = nfeItemDadosFiscaisId;
        this.produtoId = produtoId;
        this.ufOrigem = ufOrigem;
        this.ufDestino = ufDestino;
        this.origemMercadoria = origemMercadoria;
        this.cfop = cfop;
        this.quantidade = quantidade;
    }

    public void definirParametros(String fonteValores, String fonteDadosFiscais, String fonteUfOrigem,
                                  String composicaoValorOperacao, String composicaoBaseCreditos,
                                  String arredondamento, String criterioArredondamento) {
        this.paramFonteValores = fonteValores;
        this.paramFonteDadosFiscais = fonteDadosFiscais;
        this.paramFonteUfOrigem = fonteUfOrigem;
        this.paramComposicaoValorOperacao = composicaoValorOperacao;
        this.paramComposicaoBaseCreditos = composicaoBaseCreditos;
        this.paramArredondamento = arredondamento;
        this.paramCriterioArredondamento = criterioArredondamento;
    }

    public void definirComponentes(BigDecimal valorProduto, BigDecimal valorIpi, BigDecimal valorFrete,
                                   BigDecimal valorSeguro, BigDecimal valorOutrasDespesas, BigDecimal valorDesconto) {
        this.valorProduto = valorProduto;
        this.valorIpi = valorIpi;
        this.valorFrete = valorFrete;
        this.valorSeguro = valorSeguro;
        this.valorOutrasDespesas = valorOutrasDespesas;
        this.valorDesconto = valorDesconto;
    }

    public void definirAliquotasOperacao(BigDecimal icms, BigDecimal ipi, BigDecimal pis, BigDecimal cofins) {
        this.aliquotaIcmsOperacao = icms;
        this.aliquotaIpiOperacao = ipi;
        this.aliquotaPisOperacao = pis;
        this.aliquotaCofinsOperacao = cofins;
    }

    public void definirResultado(StatusCalculo status, BigDecimal valorOperacao, BigDecimal baseCreditos,
                                 BigDecimal totalCreditosSemArredondamento, BigDecimal totalCreditos,
                                 BigDecimal diferencaArredondamento, BigDecimal custoEfetivo,
                                 List<CreditoCalculo> creditos, List<PendenciaCalculo> pendencias) {
        this.status = status;
        this.valorOperacao = valorOperacao;
        this.baseCreditos = baseCreditos;
        this.totalCreditosSemArredondamento = totalCreditosSemArredondamento;
        this.totalCreditos = totalCreditos;
        this.diferencaArredondamento = diferencaArredondamento;
        this.custoEfetivo = custoEfetivo;
        this.creditos = new ArrayList<>(creditos);
        this.pendencias = new ArrayList<>(pendencias);
    }

    public Long getId() {
        return id;
    }

    public StatusCalculo getStatus() {
        return status;
    }

    public Instant getExecutadoEm() {
        return executadoEm;
    }

    public Long getExecutadoPorId() {
        return executadoPorId;
    }

    public Long getNfeItemId() {
        return nfeItemId;
    }

    public Long getNfeItemDadosFiscaisId() {
        return nfeItemDadosFiscaisId;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public TipoFornecedor getTipoFornecedor() {
        return tipoFornecedor;
    }

    public Uf getUfOrigem() {
        return ufOrigem;
    }

    public Uf getUfDestino() {
        return ufDestino;
    }

    public String getOrigemMercadoria() {
        return origemMercadoria;
    }

    public String getCfop() {
        return cfop;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getParamFonteValores() {
        return paramFonteValores;
    }

    public String getParamFonteDadosFiscais() {
        return paramFonteDadosFiscais;
    }

    public String getParamFonteUfOrigem() {
        return paramFonteUfOrigem;
    }

    public String getParamComposicaoValorOperacao() {
        return paramComposicaoValorOperacao;
    }

    public String getParamComposicaoBaseCreditos() {
        return paramComposicaoBaseCreditos;
    }

    public String getParamArredondamento() {
        return paramArredondamento;
    }

    public String getParamCriterioArredondamento() {
        return paramCriterioArredondamento;
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

    public BigDecimal getAliquotaIcmsOperacao() {
        return aliquotaIcmsOperacao;
    }

    public BigDecimal getAliquotaIpiOperacao() {
        return aliquotaIpiOperacao;
    }

    public BigDecimal getAliquotaPisOperacao() {
        return aliquotaPisOperacao;
    }

    public BigDecimal getAliquotaCofinsOperacao() {
        return aliquotaCofinsOperacao;
    }

    public BigDecimal getValorOperacao() {
        return valorOperacao;
    }

    public BigDecimal getBaseCreditos() {
        return baseCreditos;
    }

    public BigDecimal getTotalCreditosSemArredondamento() {
        return totalCreditosSemArredondamento;
    }

    public BigDecimal getTotalCreditos() {
        return totalCreditos;
    }

    public BigDecimal getDiferencaArredondamento() {
        return diferencaArredondamento;
    }

    public BigDecimal getCustoEfetivo() {
        return custoEfetivo;
    }

    public List<CreditoCalculo> getCreditos() {
        return Collections.unmodifiableList(creditos);
    }

    public List<PendenciaCalculo> getPendencias() {
        return Collections.unmodifiableList(pendencias);
    }
}
