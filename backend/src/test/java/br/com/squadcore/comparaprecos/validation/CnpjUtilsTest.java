package br.com.squadcore.comparaprecos.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** CNPJs fictícios, gerados somente para teste. */
class CnpjUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000181",
            "11.222.333/0001-81",
            " 12.345.678/0001-95 ",
            // CNPJ alfanumérico (exemplo da Receita Federal), com e sem máscara, maiúsculas e minúsculas
            "12ABC34501DE35",
            "12.ABC.345/01DE-35",
            "12.abc.345/01de-35"
    })
    void aceitaCnpjValido(String cnpj) {
        assertThat(CnpjUtils.isValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "11222333000182",      // dígito verificador errado
            "11222333000191",
            "12ABC34501DE36",
            "1122233300018",       // 13 caracteres
            "112223330001810",     // 15 caracteres
            "00000000000000",      // sequência repetida
            "11111111111111",
            "12ABC34501DEAB",      // dígitos verificadores devem ser numéricos
            "11#222333000181",
            "abc"
    })
    void rejeitaCnpjInvalido(String cnpj) {
        assertThat(CnpjUtils.isValido(cnpj)).isFalse();
    }

    @Test
    void normalizaRemovendoMascaraEConvertendoParaMaiusculas() {
        assertThat(CnpjUtils.normalizar("11.222.333/0001-81")).isEqualTo("11222333000181");
        assertThat(CnpjUtils.normalizar(" 12.abc.345/01de-35 ")).isEqualTo("12ABC34501DE35");
        assertThat(CnpjUtils.normalizar(null)).isNull();
    }
}
