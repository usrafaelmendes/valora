package br.com.squadcore.comparaprecos.exception;

public class RegraTributariaNaoEncontradaException extends RuntimeException {

    public RegraTributariaNaoEncontradaException() {
        super("Regra tributária não encontrada.");
    }
}
