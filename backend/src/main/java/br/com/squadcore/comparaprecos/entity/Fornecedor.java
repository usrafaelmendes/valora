package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Fornecedor (RF03). Tabela criada pela migration V2__cria_tabela_fornecedor.sql.
 * Guarda somente dados cadastrais; créditos e regras tributárias ficam nas regras configuráveis.
 */
@Entity
@Table(name = "fornecedor")
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    /** CNPJ normalizado: 14 caracteres, sem máscara, letras em maiúsculas. */
    @Column(nullable = false, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Uf uf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFornecedor tipo;

    /** Condição/prazo de pagamento base, em texto livre (ex.: "28/56/84 dias"); pode ser nulo. */
    @Column(name = "prazo_pagamento_base", length = 100)
    private String prazoPagamentoBase;

    /** Marcado como false na desativação; o registro nunca é apagado. */
    @Column(nullable = false)
    private boolean ativo = true;

    /** Escrito pelo Hibernate no INSERT e nunca alterado depois (updatable = false). */
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Fornecedor() {
        // exigido pelo JPA
    }

    public Fornecedor(String razaoSocial, String cnpj, Uf uf, TipoFornecedor tipo, String prazoPagamentoBase) {
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.uf = uf;
        this.tipo = tipo;
        this.prazoPagamentoBase = prazoPagamentoBase;
    }

    public Long getId() {
        return id;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Uf getUf() {
        return uf;
    }

    public void setUf(Uf uf) {
        this.uf = uf;
    }

    public TipoFornecedor getTipo() {
        return tipo;
    }

    public void setTipo(TipoFornecedor tipo) {
        this.tipo = tipo;
    }

    public String getPrazoPagamentoBase() {
        return prazoPagamentoBase;
    }

    public void setPrazoPagamentoBase(String prazoPagamentoBase) {
        this.prazoPagamentoBase = prazoPagamentoBase;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
