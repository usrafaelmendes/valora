package br.com.squadcore.comparaprecos.exception;

public class ProdutoJaCadastradoException extends RuntimeException {

    private ProdutoJaCadastradoException(String mensagem) {
        super(mensagem);
    }

    public static ProdutoJaCadastradoException porNome(boolean produtoDesativado) {
        return new ProdutoJaCadastradoException(produtoDesativado
                ? "Já existe um produto desativado cadastrado com este nome."
                : "Já existe um produto cadastrado com este nome.");
    }

    public static ProdutoJaCadastradoException porGtin(boolean produtoDesativado) {
        return new ProdutoJaCadastradoException(produtoDesativado
                ? "Já existe um produto desativado cadastrado com este GTIN/EAN."
                : "Já existe um produto cadastrado com este GTIN/EAN.");
    }
}
