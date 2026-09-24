package br.com.squadcore.comparaprecos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Valor de um parâmetro no momento da comparação (tabela comparacao_parametro). Nulo = não definido. */
@Embeddable
public class ParametroComparacao {

    @Column(nullable = false, length = 40)
    private String chave;

    @Column(length = 500)
    private String valor;

    protected ParametroComparacao() {
        // exigido pelo JPA
    }

    public ParametroComparacao(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public String getChave() {
        return chave;
    }

    public String getValor() {
        return valor;
    }
}
