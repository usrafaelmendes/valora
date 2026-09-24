package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Parâmetro geral do cálculo alterável pelo ADMIN. Tabela criada pela migration V5.
 * As chaves são fixas ({@link ChaveParametro}); os valores são dados, e nulo significa
 * "ainda não definido". Os parâmetros são criados pela migration V6.
 */
@Entity
@Table(name = "parametro_calculo")
public class ParametroCalculo {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private ChaveParametro chave;

    @Column(length = 500)
    private String valor;

    @Version
    @Column(nullable = false)
    private int versao;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ParametroCalculo() {
        // exigido pelo JPA
    }

    public ParametroCalculo(ChaveParametro chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public ChaveParametro getChave() {
        return chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public int getVersao() {
        return versao;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
