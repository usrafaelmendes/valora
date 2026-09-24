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

import java.math.BigDecimal;

/**
 * Item (grupo det) de uma NF-e importada. Tabela criada pela migration V4__cria_tabelas_nfe.sql.
 *
 * Os campos de ICMS, IPI, PIS e COFINS guardam os valores originais do XML e ficam nulos
 * quando o campo não existe na nota. Não são alíquotas de crédito: as regras que definem
 * os créditos serão aplicadas nas etapas de regras tributárias e cálculo.
 */
@Entity
@Table(name = "nfe_item")
public class NfeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nfe_id", nullable = false, updatable = false)
    private Nfe nfe;

    @Column(name = "numero_item", nullable = false, updatable = false)
    private int numeroItem;

    /** Produto cadastrado com o mesmo GTIN; nulo quando não há vínculo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(name = "codigo_produto_fornecedor", nullable = false, length = 60, updatable = false)
    private String codigoProdutoFornecedor;

    @Column(length = 14, updatable = false)
    private String gtin;

    @Column(nullable = false, length = 120, updatable = false)
    private String descricao;

    @Column(nullable = false, length = 8, updatable = false)
    private String ncm;

    @Column(nullable = false, length = 4, updatable = false)
    private String cfop;

    @Column(nullable = false, length = 6, updatable = false)
    private String unidade;

    @Column(nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 21, scale = 10, updatable = false)
    private BigDecimal valorUnitario;

    @Column(name = "valor_produto", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorProduto;

    /** Componentes de valor do item (vFrete, vSeg, vDesc, vOutro); nulos quando ausentes no XML. */
    @Column(name = "valor_frete", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorFrete;

    @Column(name = "valor_seguro", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorSeguro;

    @Column(name = "valor_desconto", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorDesconto;

    @Column(name = "valor_outras_despesas", precision = 15, scale = 2, updatable = false)
    private BigDecimal valorOutrasDespesas;

    @Column(name = "icms_origem", length = 1, updatable = false)
    private String icmsOrigem;

    @Column(name = "icms_cst", length = 2, updatable = false)
    private String icmsCst;

    @Column(name = "icms_csosn", length = 3, updatable = false)
    private String icmsCsosn;

    @Column(name = "icms_base_calculo", precision = 15, scale = 2, updatable = false)
    private BigDecimal icmsBaseCalculo;

    @Column(name = "icms_aliquota", precision = 7, scale = 4, updatable = false)
    private BigDecimal icmsAliquota;

    @Column(name = "icms_valor", precision = 15, scale = 2, updatable = false)
    private BigDecimal icmsValor;

    @Column(name = "ipi_cst", length = 2, updatable = false)
    private String ipiCst;

    @Column(name = "ipi_base_calculo", precision = 15, scale = 2, updatable = false)
    private BigDecimal ipiBaseCalculo;

    @Column(name = "ipi_aliquota", precision = 7, scale = 4, updatable = false)
    private BigDecimal ipiAliquota;

    @Column(name = "ipi_valor", precision = 15, scale = 2, updatable = false)
    private BigDecimal ipiValor;

    @Column(name = "pis_cst", length = 2, updatable = false)
    private String pisCst;

    @Column(name = "pis_base_calculo", precision = 15, scale = 2, updatable = false)
    private BigDecimal pisBaseCalculo;

    @Column(name = "pis_aliquota", precision = 7, scale = 4, updatable = false)
    private BigDecimal pisAliquota;

    @Column(name = "pis_valor", precision = 15, scale = 2, updatable = false)
    private BigDecimal pisValor;

    @Column(name = "cofins_cst", length = 2, updatable = false)
    private String cofinsCst;

    @Column(name = "cofins_base_calculo", precision = 15, scale = 2, updatable = false)
    private BigDecimal cofinsBaseCalculo;

    @Column(name = "cofins_aliquota", precision = 7, scale = 4, updatable = false)
    private BigDecimal cofinsAliquota;

    @Column(name = "cofins_valor", precision = 15, scale = 2, updatable = false)
    private BigDecimal cofinsValor;

    protected NfeItem() {
        // exigido pelo JPA
    }

    public NfeItem(int numeroItem, Produto produto, String codigoProdutoFornecedor, String gtin,
                   String descricao, String ncm, String cfop, String unidade, BigDecimal quantidade,
                   BigDecimal valorUnitario, BigDecimal valorProduto) {
        this.numeroItem = numeroItem;
        this.produto = produto;
        this.codigoProdutoFornecedor = codigoProdutoFornecedor;
        this.gtin = gtin;
        this.descricao = descricao;
        this.ncm = ncm;
        this.cfop = cfop;
        this.unidade = unidade;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.valorProduto = valorProduto;
    }

    void associarNfe(Nfe nfe) {
        this.nfe = nfe;
    }

    public void definirComponentes(BigDecimal valorFrete, BigDecimal valorSeguro, BigDecimal valorDesconto,
                                   BigDecimal valorOutrasDespesas) {
        this.valorFrete = valorFrete;
        this.valorSeguro = valorSeguro;
        this.valorDesconto = valorDesconto;
        this.valorOutrasDespesas = valorOutrasDespesas;
    }

    public void definirIcms(String origem, String cst, String csosn, BigDecimal baseCalculo,
                            BigDecimal aliquota, BigDecimal valor) {
        this.icmsOrigem = origem;
        this.icmsCst = cst;
        this.icmsCsosn = csosn;
        this.icmsBaseCalculo = baseCalculo;
        this.icmsAliquota = aliquota;
        this.icmsValor = valor;
    }

    public void definirIpi(String cst, BigDecimal baseCalculo, BigDecimal aliquota, BigDecimal valor) {
        this.ipiCst = cst;
        this.ipiBaseCalculo = baseCalculo;
        this.ipiAliquota = aliquota;
        this.ipiValor = valor;
    }

    public void definirPis(String cst, BigDecimal baseCalculo, BigDecimal aliquota, BigDecimal valor) {
        this.pisCst = cst;
        this.pisBaseCalculo = baseCalculo;
        this.pisAliquota = aliquota;
        this.pisValor = valor;
    }

    public void definirCofins(String cst, BigDecimal baseCalculo, BigDecimal aliquota, BigDecimal valor) {
        this.cofinsCst = cst;
        this.cofinsBaseCalculo = baseCalculo;
        this.cofinsAliquota = aliquota;
        this.cofinsValor = valor;
    }

    public Long getId() {
        return id;
    }

    public Nfe getNfe() {
        return nfe;
    }

    public int getNumeroItem() {
        return numeroItem;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getCodigoProdutoFornecedor() {
        return codigoProdutoFornecedor;
    }

    public String getGtin() {
        return gtin;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getNcm() {
        return ncm;
    }

    public String getCfop() {
        return cfop;
    }

    public String getUnidade() {
        return unidade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public BigDecimal getValorProduto() {
        return valorProduto;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public BigDecimal getValorSeguro() {
        return valorSeguro;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorOutrasDespesas() {
        return valorOutrasDespesas;
    }

    public String getIcmsOrigem() {
        return icmsOrigem;
    }

    public String getIcmsCst() {
        return icmsCst;
    }

    public String getIcmsCsosn() {
        return icmsCsosn;
    }

    public BigDecimal getIcmsBaseCalculo() {
        return icmsBaseCalculo;
    }

    public BigDecimal getIcmsAliquota() {
        return icmsAliquota;
    }

    public BigDecimal getIcmsValor() {
        return icmsValor;
    }

    public String getIpiCst() {
        return ipiCst;
    }

    public BigDecimal getIpiBaseCalculo() {
        return ipiBaseCalculo;
    }

    public BigDecimal getIpiAliquota() {
        return ipiAliquota;
    }

    public BigDecimal getIpiValor() {
        return ipiValor;
    }

    public String getPisCst() {
        return pisCst;
    }

    public BigDecimal getPisBaseCalculo() {
        return pisBaseCalculo;
    }

    public BigDecimal getPisAliquota() {
        return pisAliquota;
    }

    public BigDecimal getPisValor() {
        return pisValor;
    }

    public String getCofinsCst() {
        return cofinsCst;
    }

    public BigDecimal getCofinsBaseCalculo() {
        return cofinsBaseCalculo;
    }

    public BigDecimal getCofinsAliquota() {
        return cofinsAliquota;
    }

    public BigDecimal getCofinsValor() {
        return cofinsValor;
    }
}
