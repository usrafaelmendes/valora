package br.com.squadcore.comparaprecos.exception;

public class NfeNaoEncontradaException extends RuntimeException {

    public NfeNaoEncontradaException() {
        super("NF-e não encontrada.");
    }
}
