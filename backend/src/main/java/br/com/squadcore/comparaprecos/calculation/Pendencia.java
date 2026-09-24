package br.com.squadcore.comparaprecos.calculation;

public record Pendencia(TipoPendencia tipo, String mensagem) {

    public boolean bloqueante() {
        return tipo.isBloqueante();
    }
}
