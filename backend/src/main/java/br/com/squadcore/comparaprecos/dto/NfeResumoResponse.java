package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.Nfe;

import java.math.BigDecimal;
import java.time.Instant;

/** Linha da listagem de NF-e importadas (sem os itens). */
public record NfeResumoResponse(Long id, String chaveAcesso, int numero, int serie, Instant dataEmissao,
                                String naturezaOperacao, Long fornecedorId, String fornecedorRazaoSocial,
                                BigDecimal valorTotal, Instant importadoEm) {

    public static NfeResumoResponse de(Nfe nfe) {
        return new NfeResumoResponse(nfe.getId(), nfe.getChaveAcesso(), nfe.getNumero(), nfe.getSerie(),
                nfe.getDataEmissao(), nfe.getNaturezaOperacao(), nfe.getFornecedor().getId(),
                nfe.getFornecedor().getRazaoSocial(), nfe.getValorTotal(), nfe.getImportadoEm());
    }
}
