package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Produto;

public record ProdutoResponse(Long id, String nome, String descricao, String gtin, boolean ativo) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getDescricao(),
                produto.getGtin(), produto.isAtivo());
    }
}
