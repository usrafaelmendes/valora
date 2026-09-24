package br.com.squadcore.comparaprecos.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** GTINs de exemplo públicos (GS1/Wikipedia) ou calculados para teste. */
class GtinUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "96385074",           // GTIN-8
            "036000291452",       // GTIN-12 (UPC-A)
            "4006381333931",      // GTIN-13 (EAN-13)
            "7891000315507",      // GTIN-13 com prefixo brasileiro
            "17891000315504",     // GTIN-14
            " 4006381333931 "
    })
    void aceitaGtinValido(String gtin) {
        assertThat(GtinUtils.isValido(gtin)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "4006381333932",      // dígito verificador errado
            "96385075",
            "1234567",            // 7 dígitos
            "12345678901",        // 11 dígitos
            "123456789012345",    // 15 dígitos
            "00000000000000",     // somente zeros
            "SEM GTIN",           // valor usado na NF-e quando não há GTIN
            "400638133393A",
            "4006-381333931"
    })
    void rejeitaGtinInvalido(String gtin) {
        assertThat(GtinUtils.isValido(gtin)).isFalse();
    }

    @Test
    void normalizaRemovendoEspacosETratandoBrancoComoAusente() {
        assertThat(GtinUtils.normalizar(" 4006381333931 ")).isEqualTo("4006381333931");
        assertThat(GtinUtils.normalizar("   ")).isNull();
        assertThat(GtinUtils.normalizar(null)).isNull();
    }
}
