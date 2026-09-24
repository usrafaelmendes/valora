package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Cotação (RF08): necessidade de compra de um produto, na quantidade informada, e as opções
 * de fornecedores a comparar. Tabela criada pela migration V8.
 *
 * Os dados da cotação e das opções não são alterados nem apagados: novas opções podem ser
 * incluídas, e cada comparação executada fica registrada separadamente (Comparacao).
 */
@Entity
@Table(name = "cotacao")
public class Cotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal quantidade;

    @Column(length = 500, updatable = false)
    private String descricao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "criado_por_id", nullable = false, updatable = false)
    private Long criadoPorId;

    /** Ordem de cadastro (id): também é o critério de desempate da comparação. */
    @OneToMany(mappedBy = "cotacao", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<CotacaoOpcao> opcoes = new ArrayList<>();

    protected Cotacao() {
        // exigido pelo JPA
    }

    public Cotacao(Produto produto, BigDecimal quantidade, String descricao, Long criadoPorId) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.descricao = descricao;
        this.criadoPorId = criadoPorId;
    }

    public void adicionarOpcao(CotacaoOpcao opcao) {
        opcao.associarCotacao(this);
        opcoes.add(opcao);
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Long getCriadoPorId() {
        return criadoPorId;
    }

    public List<CotacaoOpcao> getOpcoes() {
        return Collections.unmodifiableList(opcoes);
    }
}
