package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Regra de crédito tributário configurável pelo ADMIN (REGRAS_TRIBUTARIAS §9).
 * Tabelas criadas pela migration V5__cria_tabelas_regras_tributarias.sql.
 *
 * A regra diz como obter a alíquota do crédito de um tributo (percentual fixo, alíquota da
 * NF-e ou sem crédito, multiplicada pelo fator) e em quais operações ela vale. Condição nula
 * ou lista vazia significa "qualquer valor". Os valores são dados: nada fica fixo no código.
 */
@Entity
@Table(name = "regra_tributaria")
public class RegraTributaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 1000)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tributo tributo;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_aliquota", nullable = false, length = 20)
    private FormaAliquota formaAliquota;

    /** Percentual (7.5 = 7,5%); somente para PERCENTUAL_FIXO. */
    @Column(precision = 7, scale = 4)
    private BigDecimal aliquota;

    /** Multiplicador da alíquota obtida (0.8 = 80%); nulo somente para SEM_CREDITO. */
    @Column(precision = 5, scale = 4)
    private BigDecimal fator;

    @Column(nullable = false)
    private int prioridade;

    @Column(nullable = false)
    private boolean ativa = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fornecedor", length = 20)
    private TipoFornecedor tipoFornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(name = "uf_origem", length = 2)
    private Uf ufOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "uf_destino", length = 2)
    private Uf ufDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "abrangencia_uf", length = 20)
    private AbrangenciaUf abrangenciaUf;

    /** Códigos de origem da mercadoria (tag orig do ICMS, 0 a 8); vazio = qualquer origem. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "regra_tributaria_origem", joinColumns = @JoinColumn(name = "regra_id"))
    @Column(name = "origem", nullable = false, length = 1)
    private Set<String> origensMercadoria = new TreeSet<>();

    /** CFOPs da operação; vazio = qualquer CFOP. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "regra_tributaria_cfop", joinColumns = @JoinColumn(name = "regra_id"))
    @Column(name = "cfop", nullable = false, length = 4)
    private Set<String> cfops = new TreeSet<>();

    /** Incrementada pelo Hibernate a cada alteração; o cálculo registra id + versão utilizados. */
    @Version
    @Column(nullable = false)
    private int versao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected RegraTributaria() {
        // exigido pelo JPA
    }

    public RegraTributaria(String nome, Tributo tributo, FormaAliquota formaAliquota) {
        this.nome = nome;
        this.tributo = tributo;
        this.formaAliquota = formaAliquota;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Tributo getTributo() {
        return tributo;
    }

    public void setTributo(Tributo tributo) {
        this.tributo = tributo;
    }

    public FormaAliquota getFormaAliquota() {
        return formaAliquota;
    }

    public void setFormaAliquota(FormaAliquota formaAliquota) {
        this.formaAliquota = formaAliquota;
    }

    public BigDecimal getAliquota() {
        return aliquota;
    }

    public void setAliquota(BigDecimal aliquota) {
        this.aliquota = aliquota;
    }

    public BigDecimal getFator() {
        return fator;
    }

    public void setFator(BigDecimal fator) {
        this.fator = fator;
    }

    public int getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(int prioridade) {
        this.prioridade = prioridade;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public TipoFornecedor getTipoFornecedor() {
        return tipoFornecedor;
    }

    public void setTipoFornecedor(TipoFornecedor tipoFornecedor) {
        this.tipoFornecedor = tipoFornecedor;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Uf getUfOrigem() {
        return ufOrigem;
    }

    public void setUfOrigem(Uf ufOrigem) {
        this.ufOrigem = ufOrigem;
    }

    public Uf getUfDestino() {
        return ufDestino;
    }

    public void setUfDestino(Uf ufDestino) {
        this.ufDestino = ufDestino;
    }

    public AbrangenciaUf getAbrangenciaUf() {
        return abrangenciaUf;
    }

    public void setAbrangenciaUf(AbrangenciaUf abrangenciaUf) {
        this.abrangenciaUf = abrangenciaUf;
    }

    public Set<String> getOrigensMercadoria() {
        return Collections.unmodifiableSet(origensMercadoria);
    }

    /** Substitui o conteúdo (e não a coleção) para o Hibernate detectar a alteração. */
    public void definirOrigensMercadoria(Set<String> origens) {
        origensMercadoria.clear();
        origensMercadoria.addAll(origens);
    }

    public Set<String> getCfops() {
        return Collections.unmodifiableSet(cfops);
    }

    public void definirCfops(Set<String> novos) {
        cfops.clear();
        cfops.addAll(novos);
    }

    public int getVersao() {
        return versao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
