package br.com.squadcore.comparaprecos.exception;

public class SistemaJaConfiguradoException extends RuntimeException {

    public SistemaJaConfiguradoException() {
        super("O sistema já foi configurado. Entre com um usuário existente.");
    }
}
