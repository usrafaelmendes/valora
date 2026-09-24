package br.com.squadcore.comparaprecos.exception;

public class RegraTributariaJaCadastradaException extends RuntimeException {

    public RegraTributariaJaCadastradaException() {
        super("Já existe uma regra tributária cadastrada com este nome.");
    }
}
