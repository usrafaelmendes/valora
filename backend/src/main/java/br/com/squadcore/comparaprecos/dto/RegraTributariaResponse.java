package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RegraTributariaResponse(Long id, String nome, String observacao, Tributo tributo,
                                      FormaAliquota formaAliquota, BigDecimal aliquota, BigDecimal fator,
                                      int prioridade, boolean ativa, TipoFornecedor tipoFornecedor,
                                      Long fornecedorId, Long produtoId, Uf ufOrigem, Uf ufDestino,
                                      AbrangenciaUf abrangenciaUf, List<String> origensMercadoria,
                                      List<String> cfops, int versao, Instant atualizadoEm) {

    public static RegraTributariaResponse de(RegraTributaria regra) {
        return new RegraTributariaResponse(regra.getId(), regra.getNome(), regra.getObservacao(),
                regra.getTributo(), regra.getFormaAliquota(), regra.getAliquota(), regra.getFator(),
                regra.getPrioridade(), regra.isAtiva(), regra.getTipoFornecedor(),
                regra.getFornecedor() == null ? null : regra.getFornecedor().getId(),
                regra.getProduto() == null ? null : regra.getProduto().getId(),
                regra.getUfOrigem(), regra.getUfDestino(), regra.getAbrangenciaUf(),
                regra.getOrigensMercadoria().stream().sorted().toList(),
                regra.getCfops().stream().sorted().toList(),
                regra.getVersao(), regra.getAtualizadoEm());
    }
}
