package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;

import java.time.Instant;
import java.util.List;

/** valoresAceitos vazio indica valor livre validado por formato (ex.: lista de CFOPs). */
public record ParametroCalculoResponse(ChaveParametro chave, String descricao, List<String> valoresAceitos,
                                       String valor, boolean definido, int versao, Instant atualizadoEm) {

    public static ParametroCalculoResponse de(ParametroCalculo parametro) {
        return new ParametroCalculoResponse(parametro.getChave(), parametro.getChave().getDescricao(),
                parametro.getChave().getValoresAceitos(), parametro.getValor(), parametro.getValor() != null,
                parametro.getVersao(), parametro.getAtualizadoEm());
    }
}
