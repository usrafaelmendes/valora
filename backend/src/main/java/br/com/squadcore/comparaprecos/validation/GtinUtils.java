package br.com.squadcore.comparaprecos.validation;

import java.util.regex.Pattern;

/**
 * Validação técnica de GTIN/EAN (GTIN-8, GTIN-12/UPC-A, GTIN-13/EAN-13 e GTIN-14):
 * formato e dígito verificador pelo algoritmo módulo 10 da GS1.
 */
public final class GtinUtils {

    private static final Pattern FORMATO = Pattern.compile("[0-9]{8}|[0-9]{12,14}");

    private GtinUtils() {
    }

    /** Remove espaços nas pontas; GTIN em branco é tratado como não informado (null). */
    public static String normalizar(String gtin) {
        return gtin == null || gtin.isBlank() ? null : gtin.trim();
    }

    public static boolean isValido(String gtin) {
        String valor = normalizar(gtin);
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            return false;
        }
        // Zeros passam no cálculo, mas não identificam produto algum.
        if (valor.chars().allMatch(c -> c == '0')) {
            return false;
        }
        // Completar com zeros à esquerda até 14 posições não altera o dígito verificador.
        String completo = "0".repeat(14 - valor.length()) + valor;
        int soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += (completo.charAt(i) - '0') * (i % 2 == 0 ? 3 : 1);
        }
        int digito = (10 - soma % 10) % 10;
        return completo.charAt(13) - '0' == digito;
    }
}
