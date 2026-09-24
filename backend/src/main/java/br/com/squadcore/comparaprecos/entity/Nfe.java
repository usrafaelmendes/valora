package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NF-e importada (RF05 a RF07, RF14). Tabela criada pela migration V4__cria_tabelas_nfe.sql.
 * Guarda os dados originais do XML, sem regra tributária aplicada. Não é alterada após a importação.
 */
@Entity
@Table(name = "nfe")
public class Nfe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_acesso", nullable = false, length = 44, updatable = false)
    private String chaveAcesso;

    @Column(nullable = false, updatable = false)
    private int numero;

    @Column(nullable = false, updatable = false)
    private int serie;

    @Column(name = "data_emissao", nullable = false, updatable = false)
    private Instant dataEmissao;

    @Column(name = "natureza_operacao", nullable = false, length = 60, updatable = false)
    private String naturezaOperacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false, updatable = false)
    private Fornecedor fornecedor;

    @Column(name = "emitente_cnpj", nullable = false, length = 14, updatable = false)
    private String emitenteCnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "emitente_uf", nullable = false, length = 2, updatable = false)
    private Uf emitenteUf;

    @Column(name = "destinatario_cnpj", length = 14, updatable = false)
    private String destinatarioCnpj;

    /** Texto, e não o enum Uf, pois a NF-e usa "EX" para destinatário no exterior. */
    @Column(name = "destinatario_uf", length = 2, updatable = false)
    private String destinatarioUf;

    @Column(name = "valor_produtos", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorProdutos;

    @Column(name = "valor_frete", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorFrete;

    @Column(name = "valor_seguro", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorSeguro;

    @Column(name = "valor_desconto", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorDesconto;

    @Column(name = "valor_outras_despesas", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorOutrasDespesas;

    @Column(name = "valor_ipi", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorIpi;

    /** Valor total da nota (vNF). */
    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal valorTotal;

    /** Os itens são gravados junto com a nota, na mesma transação. */
    @OneToMany(mappedBy = "nfe", cascade = CascadeType.ALL)
    @OrderBy("numeroItem ASC")
    private List<NfeItem> itens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "importado_em", nullable = false, updatable = false)
    private Instant importadoEm;

    protected Nfe() {
        // exigido pelo JPA
    }

    public Nfe(String chaveAcesso, int numero, int serie, Instant dataEmissao, String naturezaOperacao,
               Fornecedor fornecedor, String emitenteCnpj, Uf emitenteUf,
               String destinatarioCnpj, String destinatarioUf) {
        this.chaveAcesso = chaveAcesso;
        this.numero = numero;
        this.serie = serie;
        this.dataEmissao = dataEmissao;
        this.naturezaOperacao = naturezaOperacao;
        this.fornecedor = fornecedor;
        this.emitenteCnpj = emitenteCnpj;
        this.emitenteUf = emitenteUf;
        this.destinatarioCnpj = destinatarioCnpj;
        this.destinatarioUf = destinatarioUf;
    }

    /** Totais do grupo ICMSTot, como informados no XML. */
    public void definirTotais(BigDecimal valorProdutos, BigDecimal valorFrete, BigDecimal valorSeguro,
                              BigDecimal valorDesconto, BigDecimal valorOutrasDespesas, BigDecimal valorIpi,
                              BigDecimal valorTotal) {
        this.valorProdutos = valorProdutos;
        this.valorFrete = valorFrete;
        this.valorSeguro = valorSeguro;
        this.valorDesconto = valorDesconto;
        this.valorOutrasDespesas = valorOutrasDespesas;
        this.valorIpi = valorIpi;
        this.valorTotal = valorTotal;
    }

    public void adicionarItem(NfeItem item) {
        item.associarNfe(this);
        itens.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public int getNumero() {
        return numero;
    }

    public int getSerie() {
        return serie;
    }

    public Instant getDataEmissao() {
        return dataEmissao;
    }

    public String getNaturezaOperacao() {
        return naturezaOperacao;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public String getEmitenteCnpj() {
        return emitenteCnpj;
    }

    public Uf getEmitenteUf() {
        return emitenteUf;
    }

    public String getDestinatarioCnpj() {
        return destinatarioCnpj;
    }

    public String getDestinatarioUf() {
        return destinatarioUf;
    }

    public BigDecimal getValorProdutos() {
        return valorProdutos;
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

    public BigDecimal getValorIpi() {
        return valorIpi;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public List<NfeItem> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public Instant getImportadoEm() {
        return importadoEm;
    }
}
