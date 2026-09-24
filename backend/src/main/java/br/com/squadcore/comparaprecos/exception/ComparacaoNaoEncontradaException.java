package br.com.squadcore.comparaprecos.exception;

public class ComparacaoNaoEncontradaException extends RuntimeException {

    public ComparacaoNaoEncontradaException() {
        super("Comparação não encontrada.");
    }
}
