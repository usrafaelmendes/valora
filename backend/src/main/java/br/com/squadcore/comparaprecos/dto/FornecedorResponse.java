package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;

/** CNPJ sai sem máscara (14 caracteres); a formatação para exibição fica com o frontend. */
public record FornecedorResponse(Long id, String razaoSocial, String cnpj, Uf uf, TipoFornecedor tipo,
                                 String prazoPagamentoBase, boolean ativo) {

    public static FornecedorResponse de(Fornecedor fornecedor) {
        return new FornecedorResponse(fornecedor.getId(), fornecedor.getRazaoSocial(), fornecedor.getCnpj(),
                fornecedor.getUf(), fornecedor.getTipo(), fornecedor.getPrazoPagamentoBase(), fornecedor.isAtivo());
    }
}
