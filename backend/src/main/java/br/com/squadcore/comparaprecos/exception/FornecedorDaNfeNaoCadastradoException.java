package br.com.squadcore.comparaprecos.exception;

/** O CNPJ do emitente da NF-e não corresponde a um fornecedor ativo; nenhum cadastro é criado a partir do XML. */
public class FornecedorDaNfeNaoCadastradoException extends RuntimeException {

    private FornecedorDaNfeNaoCadastradoException(String mensagem) {
        super(mensagem);
    }

    public static FornecedorDaNfeNaoCadastradoException naoCadastrado(String cnpj) {
        return new FornecedorDaNfeNaoCadastradoException("O emitente da NF-e (CNPJ " + cnpj
                + ") não está cadastrado como fornecedor. Cadastre o fornecedor e importe a NF-e novamente.");
    }

    public static FornecedorDaNfeNaoCadastradoException desativado(String cnpj) {
        return new FornecedorDaNfeNaoCadastradoException("O fornecedor emitente da NF-e (CNPJ " + cnpj
                + ") está desativado. A NF-e não foi importada.");
    }
}
