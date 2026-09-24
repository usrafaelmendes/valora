package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
 * Comparação executada para uma cotação (RF10, RF11). Tabela criada pela migration V8.
 *
 * Registro histórico e imutável: guarda a configuração usada em todas as opções (parâmetros e
 * regras ativas com versão), o resultado de cada opção na ordem de apresentação e uma cópia do
 * produto e da quantidade. Uma nova comparação da mesma cotação gera um novo registro.
 */
@Entity
@Table(name = "comparacao")
public class Comparacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cotacao_id", nullable = false, updatable = false)
    private Long cotacaoId;

    @CreationTimestamp
    @Column(name = "executado_em", nullable = false, updatable = false)
    private Instant executadoEm;

    @Column(name = "executado_por_id", nullable = false, updatable = false)
    private Long executadoPorId;

    @Column(name = "produto_id", nullable = false, updatable = false)
    private Long produtoId;

    @Column(name = "produto_nome", nullable = false, length = 150, updatable = false)
    private String produtoNome;

    @Column(nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "total_opcoes", nullable = false, updatable = false)
    private int totalOpcoes;

    @Column(name = "total_classificadas", nullable = false, updatable = false)
    private int totalClassificadas;

    @ElementCollection
    @CollectionTable(name = "comparacao_parametro", joinColumns = @JoinColumn(name = "comparacao_id"))
    @OrderColumn(name = "posicao")
    private List<ParametroComparacao> parametros = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "comparacao_regra", joinColumns = @JoinColumn(name = "comparacao_id"))
    @OrderColumn(name = "posicao")
    private List<RegraComparacao> regras = new ArrayList<>();

    /** Classificadas (posição 1..n) e, em seguida, as não classificadas. */
    @ElementCollection
    @CollectionTable(name = "comparacao_resultado", joinColumns = @JoinColumn(name = "comparacao_id"))
    @OrderColumn(name = "ordem")
    private List<ResultadoComparacao> resultados = new ArrayList<>();

    protected Comparacao() {
        // exigido pelo JPA
    }

    public Comparacao(Cotacao cotacao, Long executadoPorId, List<ParametroComparacao> parametros,
                      List<RegraComparacao> regras, List<ResultadoComparacao> resultados) {
        this.cotacaoId = cotacao.getId();
        this.executadoPorId = executadoPorId;
        this.produtoId = cotacao.getProduto().getId();
        this.produtoNome = cotacao.getProduto().getNome();
        this.quantidade = cotacao.getQuantidade();
        this.parametros = new ArrayList<>(parametros);
        this.regras = new ArrayList<>(regras);
        this.resultados = new ArrayList<>(resultados);
        this.totalOpcoes = resultados.size();
        this.totalClassificadas = (int) resultados.stream()
                .filter(r -> r.getSituacao() == SituacaoAlternativa.CLASSIFICADA)
                .count();
    }

    public Long getId() {
        return id;
    }

    public Long getCotacaoId() {
        return cotacaoId;
    }

    public Instant getExecutadoEm() {
        return executadoEm;
    }

    public Long getExecutadoPorId() {
        return executadoPorId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public int getTotalOpcoes() {
        return totalOpcoes;
    }

    public int getTotalClassificadas() {
        return totalClassificadas;
    }

    public List<ParametroComparacao> getParametros() {
        return Collections.unmodifiableList(parametros);
    }

    public List<RegraComparacao> getRegras() {
        return Collections.unmodifiableList(regras);
    }

    public List<ResultadoComparacao> getResultados() {
        return Collections.unmodifiableList(resultados);
    }
}
