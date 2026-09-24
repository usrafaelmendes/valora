package br.com.squadcore.comparaprecos.exception;

public class NfeJaImportadaException extends RuntimeException {

    public NfeJaImportadaException() {
        super("Esta NF-e (mesma chave de acesso) já foi importada.");
    }
}
