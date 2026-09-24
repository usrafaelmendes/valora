package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * NF-e importada com seus itens. Os valores tributários são os originais do XML
 * (null quando ausentes na nota), e não créditos calculados.
 */
public record NfeResponse(Long id, String chaveAcesso, int numero, int serie, Instant dataEmissao,
                          String naturezaOperacao, Fornecedor fornecedor, String emitenteCnpj,
                          String emitenteUf, String destinatarioCnpj, String destinatarioUf,
                          Totais totais, Instant importadoEm, int itensSemProduto, List<Item> itens) {

    public static NfeResponse de(Nfe nfe) {
        List<Item> itens = nfe.getItens().stream().map(Item::de).toList();
        int semProduto = (int) itens.stream().filter(item -> item.produto() == null).count();
        return new NfeResponse(nfe.getId(), nfe.getChaveAcesso(), nfe.getNumero(), nfe.getSerie(),
                nfe.getDataEmissao(), nfe.getNaturezaOperacao(),
                new Fornecedor(nfe.getFornecedor().getId(), nfe.getFornecedor().getRazaoSocial()),
                nfe.getEmitenteCnpj(), nfe.getEmitenteUf().name(),
                nfe.getDestinatarioCnpj(), nfe.getDestinatarioUf(),
                new Totais(nfe.getValorProdutos(), nfe.getValorFrete(), nfe.getValorSeguro(),
                        nfe.getValorDesconto(), nfe.getValorOutrasDespesas(), nfe.getValorIpi(),
                        nfe.getValorTotal()),
                nfe.getImportadoEm(), semProduto, itens);
    }

    public record Fornecedor(Long id, String razaoSocial) {
    }

    public record Totais(BigDecimal valorProdutos, BigDecimal valorFrete, BigDecimal valorSeguro,
                         BigDecimal valorDesconto, BigDecimal valorOutrasDespesas, BigDecimal valorIpi,
                         BigDecimal valorTotal) {
    }

    /** produto é null quando o item não foi vinculado a um produto cadastrado. */
    public record Item(Long id, int numeroItem, Produto produto, String codigoProdutoFornecedor, String gtin,
                       String descricao, String ncm, String cfop, String unidade, BigDecimal quantidade,
                       BigDecimal valorUnitario, BigDecimal valorProduto, BigDecimal valorFrete,
                       BigDecimal valorSeguro, BigDecimal valorDesconto, BigDecimal valorOutrasDespesas,
                       Icms icms, Tributo ipi, Tributo pis, Tributo cofins) {

        static Item de(NfeItem item) {
            Produto produto = item.getProduto() == null ? null
                    : new Produto(item.getProduto().getId(), item.getProduto().getNome());
            return new Item(item.getId(), item.getNumeroItem(), produto, item.getCodigoProdutoFornecedor(),
                    item.getGtin(), item.getDescricao(), item.getNcm(), item.getCfop(), item.getUnidade(),
                    item.getQuantidade(), item.getValorUnitario(), item.getValorProduto(),
                    item.getValorFrete(), item.getValorSeguro(), item.getValorDesconto(),
                    item.getValorOutrasDespesas(),
                    new Icms(item.getIcmsOrigem(), item.getIcmsCst(), item.getIcmsCsosn(),
                            item.getIcmsBaseCalculo(), item.getIcmsAliquota(), item.getIcmsValor()),
                    new Tributo(item.getIpiCst(), item.getIpiBaseCalculo(), item.getIpiAliquota(),
                            item.getIpiValor()),
                    new Tributo(item.getPisCst(), item.getPisBaseCalculo(), item.getPisAliquota(),
                            item.getPisValor()),
                    new Tributo(item.getCofinsCst(), item.getCofinsBaseCalculo(), item.getCofinsAliquota(),
                            item.getCofinsValor()));
        }
    }

    public record Produto(Long id, String nome) {
    }

    public record Icms(String origem, String cst, String csosn, BigDecimal baseCalculo,
                       BigDecimal aliquota, BigDecimal valor) {
    }

    public record Tributo(String cst, BigDecimal baseCalculo, BigDecimal aliquota, BigDecimal valor) {
    }
}
