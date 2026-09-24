package br.com.squadcore.comparaprecos.exception;

public class CnpjJaCadastradoException extends RuntimeException {

    public CnpjJaCadastradoException(boolean fornecedorDesativado) {
        super(fornecedorDesativado
                ? "Já existe um fornecedor desativado cadastrado com este CNPJ."
                : "Já existe um fornecedor cadastrado com este CNPJ.");
    }
}
