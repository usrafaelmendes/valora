package br.com.squadcore.comparaprecos.exception;

/**
 * Validação que depende de mais de um campo ou do banco (ex.: alíquota exigida somente para
 * PERCENTUAL_FIXO). Responde 400 no mesmo formato da validação do Bean Validation.
 */
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
