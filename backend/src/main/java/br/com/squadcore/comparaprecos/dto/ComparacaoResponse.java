package br.com.squadcore.comparaprecos.dto;

import br.com.squadcore.comparaprecos.calculation.ComparadorAlternativas;
import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.Comparacao;
import br.com.squadcore.comparaprecos.entity.ParametroComparacao;
import br.com.squadcore.comparaprecos.entity.RegraComparacao;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.entity.SituacaoAlternativa;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.service.ComparacaoDetalhada;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Comparação completa (RF10 a RF12, CT24). As opções vêm em duas listas:
 * - alternativas: classificadas, ordenadas pelo custo efetivo (posição 1 = menor custo);
 * - naoClassificadas: opções sem custo efetivo válido ou com participação não confirmada, com o motivo.
 * Cada opção traz o cálculo completo (créditos, regras e versões, parâmetros, pendências e avisos).
 */
public record ComparacaoResponse(Long id, Long cotacaoId, Instant executadoEm, Long executadoPorId,
                                 Long produtoId, String produtoNome, BigDecimal quantidade,
                                 String criterioOrdenacao, int totalOpcoes, int totalClassificadas,
                                 Configuracao configuracao, List<Alternativa> alternativas,
                                 List<Alternativa> naoClassificadas) {

    public static ComparacaoResponse de(ComparacaoDetalhada detalhe) {
        Comparacao c = detalhe.comparacao();
        List<Alternativa> todas = c.getResultados().stream()
                .map(r -> Alternativa.de(r, detalhe.calculos().get(r.getCalculoId())))
                .toList();
        return new ComparacaoResponse(c.getId(), c.getCotacaoId(), c.getExecutadoEm(), c.getExecutadoPorId(),
                c.getProdutoId(), c.getProdutoNome(), c.getQuantidade(), ComparadorAlternativas.CRITERIO,
                c.getTotalOpcoes(), c.getTotalClassificadas(),
                new Configuracao(c.getParametros().stream().map(Parametro::de).toList(),
                        c.getRegras().stream().map(Regra::de).toList()),
                todas.stream().filter(a -> a.situacao() == SituacaoAlternativa.CLASSIFICADA).toList(),
                todas.stream().filter(a -> a.situacao() != SituacaoAlternativa.CLASSIFICADA).toList());
    }

    /** Configuração usada em todas as opções: parâmetros (nulo = não definido) e regras ativas. */
    public record Configuracao(List<Parametro> parametros, List<Regra> regrasAtivas) {
    }

    public record Parametro(String chave, String valor) {

        static Parametro de(ParametroComparacao p) {
            return new Parametro(p.getChave(), p.getValor());
        }
    }

    public record Regra(Long id, int versao, String nome, String tributo, int prioridade) {

        static Regra de(RegraComparacao r) {
            return new Regra(r.getRegraId(), r.getRegraVersao(), r.getRegraNome(), r.getTributo(), r.getPrioridade());
        }
    }

    public record Fornecedor(Long id, String razaoSocial, TipoFornecedor tipo, boolean ativo) {
    }

    /**
     * @param posicao  posição no ranking; nula quando não classificada
     * @param empate   mesmo custo efetivo de outra opção classificada
     * @param calculo  cálculo completo; nulo quando a opção não foi calculada
     */
    public record Alternativa(Integer posicao, boolean empate, SituacaoAlternativa situacao, String motivo,
                              Long opcaoId, Fornecedor fornecedor, boolean produtoAtivo, String condicaoPagamento,
                              String prazoPagamentoBase, BigDecimal quantidade, StatusCalculo statusCalculo,
                              BigDecimal valorOperacao, BigDecimal totalCreditos, BigDecimal custoEfetivo,
                              CalculoResponse calculo) {

        static Alternativa de(ResultadoComparacao r, CalculoCusto calculo) {
            return new Alternativa(r.getPosicao(), r.isEmpate(), r.getSituacao(), r.getMotivo(), r.getOpcaoId(),
                    new Fornecedor(r.getFornecedorId(), r.getFornecedorRazaoSocial(), r.getTipoFornecedor(),
                            r.isFornecedorAtivo()),
                    r.isProdutoAtivo(), r.getCondicaoPagamento(), r.getPrazoPagamentoBase(), r.getQuantidade(),
                    r.getStatusCalculo(), r.getValorOperacao(), r.getTotalCreditos(), r.getCustoEfetivo(),
                    calculo == null ? null : CalculoResponse.de(calculo));
        }
    }
}
