package br.com.squadcore.comparaprecos.exception;

public class CalculoNaoEncontradoException extends RuntimeException {

    public CalculoNaoEncontradoException() {
        super("Cálculo não encontrado.");
    }
}
