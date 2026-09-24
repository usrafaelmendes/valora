package br.com.squadcore.comparaprecos.exception;

/** Arquivo vazio, XML malformado, documento que não é NF-e ou NF-e com campo obrigatório ausente/inválido. */
public class NfeInvalidaException extends RuntimeException {

    public NfeInvalidaException(String mensagem) {
        super(mensagem);
    }
}
