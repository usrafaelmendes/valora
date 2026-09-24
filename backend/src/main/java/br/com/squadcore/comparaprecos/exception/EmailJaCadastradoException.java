package br.com.squadcore.comparaprecos.exception;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Já existe um usuário cadastrado com este e-mail.");
    }
}
