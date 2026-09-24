package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.CreditoCalculo;
import br.com.squadcore.comparaprecos.entity.PendenciaCalculo;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Cálculo completo, com tudo o que explica o resultado: operação, parâmetros vigentes,
 * valores e alíquotas usados, crédito de cada tributo (com a regra e a versão utilizadas),
 * totais, diferença de arredondamento, pendências e avisos.
 */
public record CalculoResponse(Long id, StatusCalculo status, Instant executadoEm, Long executadoPorId,
                              Operacao operacao, Parametros parametros, Componentes componentes,
                              AliquotasOperacao aliquotasOperacao, BigDecimal valorOperacao,
                              BigDecimal baseCreditos, List<Credito> creditos,
                              BigDecimal totalCreditosSemArredondamento, BigDecimal totalCreditos,
                              BigDecimal diferencaArredondamento, BigDecimal custoEfetivo,
                              List<Pendencia> pendencias) {

    public static CalculoResponse de(CalculoCusto c) {
        return new CalculoResponse(c.getId(), c.getStatus(), c.getExecutadoEm(), c.getExecutadoPorId(),
                new Operacao(c.getNfeItemId(), c.getNfeItemDadosFiscaisId(), c.getFornecedorId(), c.getProdutoId(),
                        c.getTipoFornecedor(), c.getUfOrigem(), c.getUfDestino(), c.getOrigemMercadoria(),
                        c.getCfop(), c.getQuantidade()),
                new Parametros(c.getParamFonteValores(), c.getParamFonteDadosFiscais(), c.getParamFonteUfOrigem(),
                        c.getParamComposicaoValorOperacao(), c.getParamComposicaoBaseCreditos(),
                        c.getParamArredondamento(), c.getParamCriterioArredondamento()),
                new Componentes(c.getValorProduto(), c.getValorIpi(), c.getValorFrete(), c.getValorSeguro(),
                        c.getValorOutrasDespesas(), c.getValorDesconto()),
                new AliquotasOperacao(c.getAliquotaIcmsOperacao(), c.getAliquotaIpiOperacao(),
                        c.getAliquotaPisOperacao(), c.getAliquotaCofinsOperacao()),
                c.getValorOperacao(), c.getBaseCreditos(),
                c.getCreditos().stream().map(Credito::de).toList(),
                c.getTotalCreditosSemArredondamento(), c.getTotalCreditos(), c.getDiferencaArredondamento(),
                c.getCustoEfetivo(),
                c.getPendencias().stream().map(Pendencia::de).toList());
    }

    public record Operacao(Long nfeItemId, Long nfeItemDadosFiscaisId, Long fornecedorId, Long produtoId,
                           TipoFornecedor tipoFornecedor, Uf ufOrigem, Uf ufDestino, String origemMercadoria,
                           String cfop, BigDecimal quantidade) {
    }

    /** Valores dos parâmetros no momento do cálculo (nulo = não definido naquele momento). */
    public record Parametros(String fonteValoresOperacao, String fonteDadosFiscais, String fonteUfOrigem,
                             String composicaoValorOperacao, String composicaoBaseCreditos,
                             String arredondamentoCreditos, String criterioArredondamento) {
    }

    public record Componentes(BigDecimal valorProduto, BigDecimal valorIpi, BigDecimal valorFrete,
                              BigDecimal valorSeguro, BigDecimal valorOutrasDespesas, BigDecimal valorDesconto) {
    }

    public record AliquotasOperacao(BigDecimal icms, BigDecimal ipi, BigDecimal pis, BigDecimal cofins) {
    }

    public record Credito(String tributo, String situacao, Regra regra, String regrasEmConflito,
                          BigDecimal aliquotaObtida, BigDecimal fator, BigDecimal aliquotaAplicada,
                          BigDecimal baseCalculo, BigDecimal valorSemArredondamento, BigDecimal valor,
                          String mensagem) {

        static Credito de(CreditoCalculo c) {
            Regra regra = c.getRegraId() == null ? null
                    : new Regra(c.getRegraId(), c.getRegraVersao(), c.getRegraNome(), c.getFormaAliquota());
            return new Credito(c.getTributo(), c.getSituacao(), regra, c.getRegrasEmConflito(),
                    c.getAliquotaObtida(), c.getFator(), c.getAliquotaAplicada(), c.getBaseCalculo(),
                    c.getValorSemArredondamento(), c.getValor(), c.getMensagem());
        }
    }

    /** Cópia da regra no momento do cálculo. */
    public record Regra(Long id, Integer versao, String nome, String formaAliquota) {
    }

    public record Pendencia(String tipo, boolean bloqueante, String mensagem) {

        static Pendencia de(PendenciaCalculo p) {
            return new Pendencia(p.getTipo(), p.isBloqueante(), p.getMensagem());
        }
    }
}
