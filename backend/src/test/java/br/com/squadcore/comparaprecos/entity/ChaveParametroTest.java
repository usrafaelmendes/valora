package br.com.squadcore.comparaprecos.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChaveParametroTest {

    @Test
    void valorEmBrancoVoltaANaoDefinido() {
        for (ChaveParametro chave : ChaveParametro.values()) {
            assertThat(chave.normalizar(null)).isNull();
            assertThat(chave.normalizar("   ")).isNull();
        }
    }

    @Test
    void ufDestinoAceitaSomenteUfsENormalizaMaiusculas() {
        assertThat(ChaveParametro.UF_DESTINO.normalizar(" go ")).isEqualTo("GO");
        assertThatThrownBy(() -> ChaveParametro.UF_DESTINO.normalizar("EX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Valor inválido. Valores aceitos: AC, AL");
    }

    @Test
    void arredondamentoAceitaSomenteAsDuasOpcoesDocumentadas() {
        assertThat(ChaveParametro.ARREDONDAMENTO_CREDITOS.getValoresAceitos())
                .containsExactly("POR_CREDITO", "SOMENTE_TOTAL");
        assertThat(ChaveParametro.ARREDONDAMENTO_CREDITOS.normalizar("por_credito")).isEqualTo("POR_CREDITO");
        assertThatThrownBy(() -> ChaveParametro.ARREDONDAMENTO_CREDITOS.normalizar("CASCATA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fonteUfOrigem() {
        assertThat(ChaveParametro.FONTE_UF_ORIGEM.normalizar("cadastro_fornecedor")).isEqualTo("CADASTRO_FORNECEDOR");
        assertThatThrownBy(() -> ChaveParametro.FONTE_UF_ORIGEM.normalizar("CNPJ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cfopsSaoNormalizadosSemRepeticaoEOrdenados() {
        assertThat(ChaveParametro.CFOPS_PARTICIPANTES.normalizar(" 2222, 1111 ,2222")).isEqualTo("1111,2222");
    }

    @Test
    void cfopsInvalidosSaoRejeitados() {
        for (String invalido : new String[]{"123", "12345", "1111,", "abcd", "1111;2222"}) {
            assertThatThrownBy(() -> ChaveParametro.CFOPS_PARTICIPANTES.normalizar(invalido))
                    .as(invalido)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("4 dígitos");
        }
    }

    // Parâmetros do cálculo (Etapa 8)

    @Test
    void composicaoDoValorAceitaListaDeComponentesNaOrdemCanonica() {
        assertThat(ChaveParametro.COMPOSICAO_VALOR_OPERACAO.normalizar(" desconto, VALOR_PRODUTO ,ipi,IPI"))
                .isEqualTo("VALOR_PRODUTO,IPI,DESCONTO");
        assertThat(ChaveParametro.componentesDe("VALOR_PRODUTO,IPI,DESCONTO"))
                .containsExactly(ComponenteValor.VALOR_PRODUTO, ComponenteValor.IPI, ComponenteValor.DESCONTO);
    }

    @Test
    void composicaoDoValorRejeitaComponenteDesconhecidoOuValorOperacao() {
        assertThatThrownBy(() -> ChaveParametro.COMPOSICAO_VALOR_OPERACAO.normalizar("VALOR_PRODUTO,ICMS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VALOR_PRODUTO, IPI, FRETE, SEGURO, OUTRAS_DESPESAS, DESCONTO");
        assertThatThrownBy(() -> ChaveParametro.COMPOSICAO_VALOR_OPERACAO.normalizar("VALOR_OPERACAO"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChaveParametro.COMPOSICAO_VALOR_OPERACAO.normalizar("VALOR_PRODUTO,"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void baseDosCreditosAceitaValorOperacaoSozinhoOuComponentes() {
        assertThat(ChaveParametro.COMPOSICAO_BASE_CREDITOS.normalizar("valor_operacao")).isEqualTo("VALOR_OPERACAO");
        assertThat(ChaveParametro.COMPOSICAO_BASE_CREDITOS.normalizar("VALOR_PRODUTO")).isEqualTo("VALOR_PRODUTO");
        assertThatThrownBy(() -> ChaveParametro.COMPOSICAO_BASE_CREDITOS.normalizar("VALOR_OPERACAO,IPI"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser combinado");
    }

    @Test
    void fontesECriterioAceitamSomenteAsOpcoesPrevistas() {
        assertThat(ChaveParametro.FONTE_VALORES_OPERACAO.normalizar("nfe_item")).isEqualTo("NFE_ITEM");
        assertThat(ChaveParametro.FONTE_DADOS_FISCAIS.normalizar("ultima_nfe_fornecedor_produto"))
                .isEqualTo("ULTIMA_NFE_FORNECEDOR_PRODUTO");
        assertThat(ChaveParametro.CRITERIO_ARREDONDAMENTO.normalizar("meio_para_par")).isEqualTo("MEIO_PARA_PAR");
        assertThatThrownBy(() -> ChaveParametro.FONTE_VALORES_OPERACAO.normalizar("COTACAO"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChaveParametro.CRITERIO_ARREDONDAMENTO.normalizar("TRUNCAR"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
