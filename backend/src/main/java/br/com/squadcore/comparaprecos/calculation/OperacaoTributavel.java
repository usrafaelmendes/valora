package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;

/**
 * Características da operação usadas para escolher as regras tributárias configuradas.
 * Qualquer campo pode ser nulo (desconhecido): nesse caso, somente regras sem condição
 * sobre esse campo podem se aplicar.
 *
 * @param origemMercadoria código de origem da mercadoria (tag orig do ICMS, 0 a 8)
 */
public record OperacaoTributavel(Long fornecedorId, TipoFornecedor tipoFornecedor, Long produtoId,
                                 Uf ufOrigem, Uf ufDestino, String origemMercadoria, String cfop) {
}
