package br.com.squadcore.comparaprecos.exception;

public class FornecedorNaoEncontradoException extends RuntimeException {

    public FornecedorNaoEncontradoException() {
        super("Fornecedor não encontrado.");
    }
}
