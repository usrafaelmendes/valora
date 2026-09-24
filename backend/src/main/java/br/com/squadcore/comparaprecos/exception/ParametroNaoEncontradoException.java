package br.com.squadcore.comparaprecos.exception;

public class ParametroNaoEncontradoException extends RuntimeException {

    public ParametroNaoEncontradoException() {
        super("Parâmetro de cálculo não encontrado.");
    }
}
