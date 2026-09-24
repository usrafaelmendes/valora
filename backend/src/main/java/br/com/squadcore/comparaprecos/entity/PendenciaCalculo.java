package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Pendência (bloqueante) ou aviso de um cálculo (tabela calculo_custo_pendencia). */
@Embeddable
public class PendenciaCalculo {

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false)
    private boolean bloqueante;

    @Column(nullable = false, length = 500)
    private String mensagem;

    protected PendenciaCalculo() {
        // exigido pelo JPA
    }

    public PendenciaCalculo(String tipo, boolean bloqueante, String mensagem) {
        this.tipo = tipo;
        this.bloqueante = bloqueante;
        this.mensagem = mensagem;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isBloqueante() {
        return bloqueante;
    }

    public String getMensagem() {
        return mensagem;
    }
}
