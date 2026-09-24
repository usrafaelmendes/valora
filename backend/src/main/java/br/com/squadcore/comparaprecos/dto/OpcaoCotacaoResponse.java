package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;

import java.time.Instant;

/** Opção como foi informada na cotação (valores e dados fiscais nulos = não informados). */
public record OpcaoCotacaoResponse(Long id, Long fornecedorId, String fornecedorRazaoSocial, Long nfeItemId,
                                   String condicaoPagamento, String observacao,
                                   CalculoRequest.ValoresInformados valores,
                                   CalculoRequest.DadosFiscaisInformados dadosFiscais, Instant criadoEm) {

    public static OpcaoCotacaoResponse de(CotacaoOpcao o) {
        CalculoRequest.ValoresInformados valores = !o.temValores() ? null : new CalculoRequest.ValoresInformados(
                o.getValorProduto(), o.getValorIpi(), o.getValorFrete(), o.getValorSeguro(),
                o.getValorOutrasDespesas(), o.getValorDesconto());
        CalculoRequest.DadosFiscaisInformados dadosFiscais = !o.temDadosFiscais() ? null
                : new CalculoRequest.DadosFiscaisInformados(o.getOrigemMercadoria(), o.getCfop(), o.getAliquotaIcms(),
                        o.getAliquotaIpi(), o.getAliquotaPis(), o.getAliquotaCofins());
        return new OpcaoCotacaoResponse(o.getId(), o.getFornecedor().getId(), o.getFornecedor().getRazaoSocial(),
                o.getNfeItemId(), o.getCondicaoPagamento(), o.getObservacao(), valores, dadosFiscais, o.getCriadoEm());
    }
}
