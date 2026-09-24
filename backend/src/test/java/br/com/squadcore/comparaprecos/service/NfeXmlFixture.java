package br.com.squadcore.comparaprecos.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Carrega a NF-e sintética de teste (src/test/resources/nfe/nfe-sintetica.xml).
 * Todos os dados da fixture são fictícios; variações são criadas por substituição de texto.
 */
public final class NfeXmlFixture {

    public static final String CHAVE = "41260911222333000181550010000012341876543218";
    public static final String CNPJ_EMITENTE = "11222333000181";
    public static final String CNPJ_DESTINATARIO = "11444777000161";
    public static final String GTIN_ITEM_1 = "4006381333931";
    public static final String GTIN_ITEM_3 = "7891000315507";

    private NfeXmlFixture() {
    }

    public static String xml() {
        try (InputStream entrada = NfeXmlFixture.class.getResourceAsStream("/nfe/nfe-sintetica.xml")) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static byte[] bytes() {
        return bytes(xml());
    }

    public static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    /** Substitui um trecho que precisa existir na fixture (evita testes que não alteram nada). */
    public static String trocar(String xml, String trecho, String substituto) {
        if (!xml.contains(trecho)) {
            throw new IllegalArgumentException("Trecho não encontrado na fixture: " + trecho);
        }
        return xml.replace(trecho, substituto);
    }

    public static String trocar(String trecho, String substituto) {
        return trocar(xml(), trecho, substituto);
    }

    /** Chave de acesso com o dígito verificador (módulo 11) calculado para os 43 primeiros dígitos. */
    public static String chaveComDigito(String chave43) {
        int soma = 0;
        int[] pesos = {2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < 43; i++) {
            soma += (chave43.charAt(42 - i) - '0') * pesos[i % 8];
        }
        int resto = soma % 11;
        return chave43 + (resto < 2 ? 0 : 11 - resto);
    }
}
