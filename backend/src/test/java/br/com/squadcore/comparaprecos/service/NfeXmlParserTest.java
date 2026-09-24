package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.exception.NfeInvalidaException;
import br.com.squadcore.comparaprecos.service.NfeXmlParser.ItemLido;
import br.com.squadcore.comparaprecos.service.NfeXmlParser.NfeLida;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CHAVE;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CNPJ_DESTINATARIO;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CNPJ_EMITENTE;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.GTIN_ITEM_1;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.GTIN_ITEM_3;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.bytes;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.trocar;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.xml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parser da NF-e (CT09, CT10, CT11) com a fixture sintética. Nenhum valor é interpretado
 * como crédito: os testes conferem somente que os dados originais do XML são preservados.
 */
class NfeXmlParserTest {

    private final NfeXmlParser parser = new NfeXmlParser();

    // CT09 — XML válido

    @Test
    void leDadosDaNota() {
        NfeLida nfe = parser.ler(bytes());

        assertThat(nfe.chaveAcesso()).isEqualTo(CHAVE);
        assertThat(nfe.numero()).isEqualTo(1234);
        assertThat(nfe.serie()).isEqualTo(1);
        assertThat(nfe.dataEmissao()).isEqualTo(Instant.parse("2026-09-10T17:30:00Z"));
        assertThat(nfe.naturezaOperacao()).isEqualTo("Venda de mercadoria (teste)");
        assertThat(nfe.emitenteCnpj()).isEqualTo(CNPJ_EMITENTE);
        assertThat(nfe.emitenteUf()).isEqualTo(Uf.PR);
        assertThat(nfe.destinatarioCnpj()).isEqualTo(CNPJ_DESTINATARIO);
        assertThat(nfe.destinatarioUf()).isEqualTo("GO");
    }

    @Test
    void leTotaisDaNotaSemAlterarValores() {
        NfeLida nfe = parser.ler(bytes());

        assertThat(nfe.valorProdutos()).isEqualByComparingTo("386.00");
        assertThat(nfe.valorFrete()).isEqualByComparingTo("10.00");
        assertThat(nfe.valorSeguro()).isEqualByComparingTo("0.00");
        assertThat(nfe.valorDesconto()).isEqualByComparingTo("5.00");
        assertThat(nfe.valorOutrasDespesas()).isEqualByComparingTo("0.00");
        assertThat(nfe.valorIpi()).isEqualByComparingTo("20.10");
        assertThat(nfe.valorTotal()).isEqualByComparingTo("411.10");
    }

    @Test
    void leMultiplosItensNaOrdemDoXml() {
        NfeLida nfe = parser.ler(bytes());

        assertThat(nfe.itens()).extracting(ItemLido::numeroItem).containsExactly(1, 2, 3);
        ItemLido item = nfe.itens().getFirst();
        assertThat(item.codigoProdutoFornecedor()).isEqualTo("TST-001");
        assertThat(item.descricao()).isEqualTo("PRODUTO TESTE X");
        assertThat(item.ncm()).isEqualTo("99999901");
        assertThat(item.cfop()).isEqualTo("6102");
        assertThat(item.unidade()).isEqualTo("UN");
        assertThat(item.quantidade()).isEqualByComparingTo("2");
        assertThat(item.valorUnitario()).isEqualByComparingTo("100.5");
        assertThat(item.valorProduto()).isEqualByComparingTo("201.00");
    }

    @Test
    void aceitaNfeSemProtocoloDeAutorizacao() {
        String semProtocolo = xml()
                .replaceAll("(?s)<protNFe.*</protNFe>", "")
                .replace("<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">", "")
                .replace("</nfeProc>", "")
                .replace("<NFe>", "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">");

        assertThat(parser.ler(bytes(semProtocolo)).chaveAcesso()).isEqualTo(CHAVE);
    }

    // Namespaces

    @Test
    void aceitaNamespaceComPrefixo() {
        String comPrefixo = xml()
                .replace("xmlns=\"http://www.portalfiscal.inf.br/nfe\"", "xmlns:nfe=\"http://www.portalfiscal.inf.br/nfe\"")
                .replaceAll("<(/?)([A-Za-z])", "<$1nfe:$2");

        NfeLida nfe = parser.ler(bytes(comPrefixo));

        assertThat(nfe.chaveAcesso()).isEqualTo(CHAVE);
        assertThat(nfe.itens()).hasSize(3);
    }

    @Test
    void rejeitaXmlSemNamespaceDaNfe() {
        String semNamespace = trocar(" xmlns=\"http://www.portalfiscal.inf.br/nfe\"", "");

        assertInvalida(semNamespace, "O XML enviado não é uma NF-e.");
    }

    @Test
    void rejeitaNamespaceDiferente() {
        String outroNamespace = trocar("http://www.portalfiscal.inf.br/nfe", "http://exemplo.teste/outro");

        assertInvalida(outroNamespace, "O XML enviado não é uma NF-e.");
    }

    @Test
    void ignoraCamposDeOutroNamespace() {
        String comCampoEstranho = trocar("<cProd>TST-001</cProd>",
                "<x:cProd xmlns:x=\"http://exemplo.teste/outro\">OUTRO</x:cProd><cProd>TST-001</cProd>");

        assertThat(parser.ler(bytes(comCampoEstranho)).itens().getFirst().codigoProdutoFornecedor())
                .isEqualTo("TST-001");
    }

    // GTIN

    @Test
    void gtinValidoEhMantido() {
        NfeLida nfe = parser.ler(bytes());

        assertThat(nfe.itens().get(0).gtin()).isEqualTo(GTIN_ITEM_1);
        assertThat(nfe.itens().get(2).gtin()).isEqualTo(GTIN_ITEM_3);
    }

    @Test
    void semGtinFicaNulo() {
        assertThat(parser.ler(bytes()).itens().get(1).gtin()).isNull();
    }

    @Test
    void gtinComDigitoVerificadorInvalidoFicaNulo() {
        String gtinInvalido = trocar("<cEAN>4006381333931</cEAN>", "<cEAN>4006381333932</cEAN>");

        assertThat(parser.ler(bytes(gtinInvalido)).itens().getFirst().gtin()).isNull();
    }

    @Test
    void cEanAusenteOuVazioFicaNulo() {
        String semTag = trocar("<cEAN>4006381333931</cEAN>", "");
        String vazio = trocar("<cEAN>4006381333931</cEAN>", "<cEAN></cEAN>");

        assertThat(parser.ler(bytes(semTag)).itens().getFirst().gtin()).isNull();
        assertThat(parser.ler(bytes(vazio)).itens().getFirst().gtin()).isNull();
    }

    // Dados tributários: preservados como no XML

    @Test
    void preservaDadosTributariosOriginais() {
        ItemLido item = parser.ler(bytes()).itens().getFirst();

        assertThat(item.icms().origem()).isEqualTo("1");
        assertThat(item.icms().cst()).isEqualTo("00");
        assertThat(item.icms().csosn()).isNull();
        assertThat(item.icms().baseCalculo()).isEqualByComparingTo("201.00");
        assertThat(item.icms().aliquota()).isEqualByComparingTo("7.00");
        assertThat(item.icms().valor()).isEqualByComparingTo("14.07");

        assertThat(item.ipi().cst()).isEqualTo("50");
        assertThat(item.ipi().baseCalculo()).isEqualByComparingTo("201.00");
        assertThat(item.ipi().aliquota()).isEqualByComparingTo("10");
        assertThat(item.ipi().valor()).isEqualByComparingTo("20.10");

        assertThat(item.pis().cst()).isEqualTo("01");
        assertThat(item.pis().aliquota()).isEqualByComparingTo("1.35");
        assertThat(item.pis().valor()).isEqualByComparingTo("2.71");
        assertThat(item.cofins().cst()).isEqualTo("01");
        assertThat(item.cofins().aliquota()).isEqualByComparingTo("5.00");
        assertThat(item.cofins().valor()).isEqualByComparingTo("10.05");
    }

    @Test
    void camposTributariosAusentesFicamNulosENaoZero() {
        ItemLido item = parser.ler(bytes()).itens().get(1);

        assertThat(item.icms().origem()).isEqualTo("0");
        assertThat(item.icms().cst()).isEqualTo("41");
        assertThat(item.icms().baseCalculo()).isNull();
        assertThat(item.icms().aliquota()).isNull();
        assertThat(item.icms().valor()).isNull();
        assertThat(item.ipi().cst()).isEqualTo("53");
        assertThat(item.ipi().aliquota()).isNull();
        assertThat(item.ipi().valor()).isNull();
        assertThat(item.pis().cst()).isEqualTo("06");
        assertThat(item.pis().valor()).isNull();
        assertThat(item.cofins().cst()).isEqualTo("06");
        assertThat(item.cofins().valor()).isNull();
    }

    @Test
    void itemSemGrupoIpiTemIpiNulo() {
        ItemLido item = parser.ler(bytes()).itens().get(2);

        assertThat(item.ipi()).isEqualTo(new NfeXmlParser.Tributo(null, null, null, null));
    }

    @Test
    void leCsosnDoSimplesNacional() {
        String simples = trocar("<ICMS40>\n              <orig>0</orig>\n              <CST>41</CST>\n            </ICMS40>",
                "<ICMSSN102><orig>2</orig><CSOSN>102</CSOSN></ICMSSN102>");

        ItemLido item = parser.ler(bytes(simples)).itens().get(1);

        assertThat(item.icms().origem()).isEqualTo("2");
        assertThat(item.icms().cst()).isNull();
        assertThat(item.icms().csosn()).isEqualTo("102");
    }

    // CT11 — Campos obrigatórios ausentes ou inválidos

    @Test
    void rejeitaCampoObrigatorioDoItemAusente() {
        String semValor = trocar("<vProd>201.00</vProd>", "");

        assertInvalida(semValor, "NF-e inválida: campo obrigatório ausente (det[nItem=1]/prod/vProd).");
    }

    @Test
    void rejeitaCampoObrigatorioDaNotaAusente() {
        assertInvalida(trocar("<nNF>1234</nNF>", ""), "NF-e inválida: campo obrigatório ausente (ide/nNF).");
        assertInvalida(trocar("<vNF>411.10</vNF>", ""),
                "NF-e inválida: campo obrigatório ausente (total/ICMSTot/vNF).");
    }

    @Test
    void rejeitaEmitenteSemCnpj() {
        String comCpf = trocar("<CNPJ>11222333000181</CNPJ>", "<CPF>12345678909</CPF>");

        assertInvalida(comCpf, "O emitente da NF-e não possui CNPJ. Somente emitentes com CNPJ são aceitos.");
    }

    @Test
    void rejeitaCnpjDoEmitenteInvalido() {
        String cnpjInvalido = trocar("<CNPJ>11222333000181</CNPJ>", "<CNPJ>11222333000182</CNPJ>");

        assertInvalida(cnpjInvalido, "NF-e inválida: campo com valor inválido (emit/CNPJ).");
    }

    @Test
    void rejeitaChaveQueNaoCorrespondeAoEmitente() {
        String outroEmitente = trocar("<CNPJ>11222333000181</CNPJ>", "<CNPJ>22333444000181</CNPJ>");

        assertInvalida(outroEmitente, "A chave de acesso não corresponde ao CNPJ do emitente.");
    }

    @Test
    void rejeitaChaveComDigitoVerificadorInvalido() {
        String chaveInvalida = trocar("Id=\"NFe" + CHAVE + "\"", "Id=\"NFe" + CHAVE.substring(0, 43) + "0\"");

        assertInvalida(chaveInvalida,
                "A chave de acesso da NF-e (atributo Id de infNFe) está ausente ou é inválida.");
    }

    @Test
    void rejeitaDecimalForaDoFormatoDoLayout() {
        assertInvalida(trocar("<vProd>201.00</vProd>", "<vProd>201,00</vProd>"),
                "NF-e inválida: campo com valor inválido (det[nItem=1]/prod/vProd).");
        assertInvalida(trocar("<vProd>201.00</vProd>", "<vProd>201.001</vProd>"),
                "NF-e inválida: campo com valor inválido (det[nItem=1]/prod/vProd).");
        assertInvalida(trocar("<vProd>201.00</vProd>", "<vProd>-201.00</vProd>"),
                "NF-e inválida: campo com valor inválido (det[nItem=1]/prod/vProd).");
    }

    @Test
    void rejeitaUfDoEmitenteInexistente() {
        assertInvalida(trocar("<UF>PR</UF>", "<UF>XX</UF>"),
                "NF-e inválida: campo com valor inválido (emit/enderEmit/UF).");
    }

    @Test
    void rejeitaDataDeEmissaoInvalida() {
        assertInvalida(trocar("2026-09-10T14:30:00-03:00", "10/09/2026"),
                "NF-e inválida: campo com valor inválido (ide/dhEmi).");
    }

    @Test
    void rejeitaTextoMaiorQueOLayout() {
        assertInvalida(trocar("<cProd>TST-001</cProd>", "<cProd>" + "X".repeat(61) + "</cProd>"),
                "NF-e inválida: campo com valor inválido (det[nItem=1]/prod/cProd).");
    }

    // Estrutura inesperada

    @Test
    void rejeitaNotaSemItens() {
        String semItens = xml().replaceAll("(?s)<det nItem=.*</det>", "");

        assertInvalida(semItens, "NF-e inválida: a nota não possui itens (det).");
    }

    @Test
    void rejeitaNumeroDeItemRepetido() {
        assertInvalida(trocar("<det nItem=\"2\">", "<det nItem=\"1\">"),
                "NF-e inválida: número de item repetido (1).");
    }

    @Test
    void rejeitaNumeroDeItemInvalido() {
        assertInvalida(trocar("<det nItem=\"2\">", "<det nItem=\"0\">"),
                "NF-e inválida: campo com valor inválido (det[nItem=0]/@nItem).");
    }

    @Test
    void rejeitaVersaoDeLayoutNaoSuportada() {
        String versaoAntiga = trocar("<infNFe Id=\"NFe" + CHAVE + "\" versao=\"4.00\">",
                "<infNFe Id=\"NFe" + CHAVE + "\" versao=\"3.10\">");

        assertInvalida(versaoAntiga, "Versão de layout da NF-e não suportada. Somente a versão 4.00 é aceita.");
    }

    @Test
    void rejeitaModeloDiferenteDe55() {
        assertInvalida(trocar("<mod>55</mod>", "<mod>65</mod>"), "Somente NF-e modelo 55 é aceita.");
    }

    @Test
    void rejeitaNfeProcSemNfe() {
        String semNfe = xml().replaceAll("(?s)<NFe>.*</NFe>", "");

        assertInvalida(semNfe, "O XML enviado não é uma NF-e.");
    }

    @Test
    void rejeitaOutroDocumentoXml() {
        assertInvalida("<?xml version=\"1.0\"?><pedido><numero>1</numero></pedido>", "O XML enviado não é uma NF-e.");
    }

    // CT10 — Arquivo inválido

    @ParameterizedTest
    @ValueSource(strings = {"isto não é xml", "<nfeProc><NFe>", "{\"json\": true}", "%PDF-1.4 conteúdo"})
    void rejeitaConteudoQueNaoEhXmlValido(String conteudo) {
        assertInvalida(conteudo, "O arquivo enviado não é um XML válido.");
    }

    @Test
    void rejeitaArquivoVazio() {
        assertThatThrownBy(() -> parser.ler(new byte[0]))
                .isInstanceOf(NfeInvalidaException.class)
                .hasMessage("O arquivo enviado está vazio.");
        assertThatThrownBy(() -> parser.ler(null))
                .isInstanceOf(NfeInvalidaException.class)
                .hasMessage("O arquivo enviado está vazio.");
    }

    // Segurança: XXE e entidades

    @Test
    void rejeitaDoctypeComEntidadeExterna() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE nfeProc [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe"><x>&xxe;</x></nfeProc>
                """;

        assertInvalida(xxe, "O arquivo enviado não é um XML válido.");
    }

    @Test
    void rejeitaExpansaoDeEntidadesInternas() {
        String bomba = """
                <?xml version="1.0"?>
                <!DOCTYPE a [<!ENTITY a "aaaaaaaaaa"><!ENTITY b "&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;">]>
                <a>&b;</a>
                """;

        assertInvalida(bomba, "O arquivo enviado não é um XML válido.");
    }

    @Test
    void digitoVerificadorDaChave() {
        assertThat(NfeXmlParser.digitoVerificadorValido(CHAVE)).isTrue();
        assertThat(NfeXmlParser.digitoVerificadorValido(CHAVE.substring(0, 43) + "9")).isFalse();
    }

    @Test
    void valoresNaoPerdemCasasDecimais() {
        ItemLido item = parser.ler(bytes()).itens().getFirst();

        assertThat(item.valorUnitario()).isEqualTo(new BigDecimal("100.5000000000"));
        assertThat(item.quantidade()).isEqualTo(new BigDecimal("2.0000"));
    }

    private void assertInvalida(String xml, String mensagem) {
        assertThatThrownBy(() -> parser.ler(bytes(xml)))
                .isInstanceOf(NfeInvalidaException.class)
                .hasMessage(mensagem);
    }

    // Componentes de valor do item (Etapa 8)

    @Test
    void leComponentesDeValorDoItemQuandoExistem() {
        String comComponentes = trocar("<vProd>201.00</vProd>",
                "<vProd>201.00</vProd><vFrete>10.50</vFrete><vSeg>1.25</vSeg><vDesc>3.00</vDesc><vOutro>0.75</vOutro>");

        ItemLido item = parser.ler(bytes(comComponentes)).itens().getFirst();

        assertThat(item.valorFrete()).isEqualByComparingTo("10.50");
        assertThat(item.valorSeguro()).isEqualByComparingTo("1.25");
        assertThat(item.valorDesconto()).isEqualByComparingTo("3.00");
        assertThat(item.valorOutrasDespesas()).isEqualByComparingTo("0.75");
    }

    @Test
    void componentesDeValorAusentesNoItemFicamNulos() {
        ItemLido item = parser.ler(bytes()).itens().getFirst();

        assertThat(item.valorFrete()).isNull();
        assertThat(item.valorSeguro()).isNull();
        assertThat(item.valorDesconto()).isNull();
        assertThat(item.valorOutrasDespesas()).isNull();
    }

    @Test
    void rejeitaComponenteDeValorDoItemInvalido() {
        String invalido = trocar("<vProd>201.00</vProd>", "<vProd>201.00</vProd><vFrete>-1.00</vFrete>");

        assertThatThrownBy(() -> parser.ler(bytes(invalido)))
                .isInstanceOf(NfeInvalidaException.class)
                .hasMessageContaining("vFrete");
    }
}
