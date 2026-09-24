package br.com.squadcore.comparaprecos.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalização e validação técnica de CNPJ (formato e dígitos verificadores).
 *
 * Aceita o CNPJ numérico e o alfanumérico (IN RFB nº 2.229/2024): 12 posições com
 * letras maiúsculas ou dígitos, seguidas de 2 dígitos verificadores numéricos.
 * O cálculo dos dígitos usa o valor ASCII do caractere menos 48, o que mantém o
 * resultado idêntico ao algoritmo tradicional para CNPJs somente numéricos.
 */
public final class CnpjUtils {

    private static final Pattern FORMATO = Pattern.compile("[0-9A-Z]{12}[0-9]{2}");
    private static final Pattern MASCARA = Pattern.compile("[.\\-/\\s]");
    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjUtils() {
    }

    /** Remove pontos, barra, hífen e espaços e converte letras para maiúsculas. */
    public static String normalizar(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return MASCARA.matcher(cnpj).replaceAll("").toUpperCase(Locale.ROOT);
    }

    /** Indica se o CNPJ (com ou sem máscara) tem formato e dígitos verificadores válidos. */
    public static boolean isValido(String cnpj) {
        String valor = normalizar(cnpj);
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            return false;
        }
        // Sequências repetidas (ex.: 00000000000000) passam no cálculo, mas não são CNPJs válidos.
        if (valor.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = digito(valor, PESOS_DV1);
        int dv2 = digito(valor, PESOS_DV2);
        return valor.charAt(12) - '0' == dv1 && valor.charAt(13) - '0' == dv2;
    }

    private static int digito(String valor, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (valor.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
