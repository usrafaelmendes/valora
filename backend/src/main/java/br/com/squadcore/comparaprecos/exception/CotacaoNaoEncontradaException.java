package br.com.squadcore.comparaprecos.exception;

public class CotacaoNaoEncontradaException extends RuntimeException {

    public CotacaoNaoEncontradaException() {
        super("Cotação não encontrada.");
    }
}
