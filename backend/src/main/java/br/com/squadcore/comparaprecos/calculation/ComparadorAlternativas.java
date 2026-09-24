package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.PendenciaCalculo;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.entity.SituacaoAlternativa;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classifica as opções de uma comparação (RF10, RF11, CT18 a CT20, CT35).
 *
 * O único critério de ordenação é o custo efetivo calculado, do menor para o maior. Em caso de
 * empate, vale a ordem de cadastro da opção na cotação (id crescente), e as opções empatadas são
 * marcadas: é um desempate técnico e determinístico, e não um critério de negócio. Prazo de
 * pagamento, preço nominal e impostos isolados não influenciam a ordem (REGRAS §8, RF11).
 *
 * Só é classificada a opção com cálculo CALCULADO cuja participação na comparação está confirmada.
 * As demais ficam fora do ranking, com a situação e o motivo; nenhuma vira custo zero.
 * Não calcula tributos: usa o resultado já gravado pelo CalculoService.
 */
@Component
public class ComparadorAlternativas {

    /** Critério apresentado na comparação. */
    public static final String CRITERIO = "Custo efetivo crescente. Empate: ordem de cadastro da opção na cotação.";

    static final int TAMANHO_MAXIMO_MOTIVO = 2000;

    private static final Comparator<CandidatoComparacao> POR_CUSTO_E_CADASTRO = Comparator
            .comparing((CandidatoComparacao c) -> c.calculo().getCustoEfetivo())
            .thenComparing(c -> c.opcao().getId());

    /** Classificadas (posição 1..n) seguidas das não classificadas, na ordem de cadastro. */
    public List<ResultadoComparacao> classificar(List<CandidatoComparacao> candidatos) {
        List<CandidatoComparacao> classificaveis = new ArrayList<>();
        List<ResultadoComparacao> naoClassificadas = new ArrayList<>();
        candidatos.stream()
                .sorted(Comparator.comparing(c -> c.opcao().getId()))
                .forEach(candidato -> {
                    List<Motivo> motivos = motivos(candidato);
                    if (motivos.isEmpty()) {
                        classificaveis.add(candidato);
                    } else {
                        naoClassificadas.add(new ResultadoComparacao(candidato.opcao(), candidato.produtoAtivo(),
                                candidato.calculo(), motivos.getFirst().situacao(), null, false, texto(motivos)));
                    }
                });

        classificaveis.sort(POR_CUSTO_E_CADASTRO);
        List<ResultadoComparacao> resultados = new ArrayList<>();
        for (int i = 0; i < classificaveis.size(); i++) {
            CandidatoComparacao candidato = classificaveis.get(i);
            boolean empate = classificaveis.stream().anyMatch(outro -> outro != candidato
                    && outro.calculo().getCustoEfetivo().compareTo(candidato.calculo().getCustoEfetivo()) == 0);
            resultados.add(new ResultadoComparacao(candidato.opcao(), candidato.produtoAtivo(), candidato.calculo(),
                    SituacaoAlternativa.CLASSIFICADA, i + 1, empate,
                    empate ? "Mesmo custo efetivo de outra opção: desempate pela ordem de cadastro da opção." : null));
        }
        resultados.addAll(naoClassificadas);
        return resultados;
    }

    private record Motivo(SituacaoAlternativa situacao, String mensagem) {
    }

    /** Motivos que impedem a classificação, do mais para o menos determinante. Vazio = classificável. */
    private static List<Motivo> motivos(CandidatoComparacao candidato) {
        List<Motivo> motivos = new ArrayList<>();
        CotacaoOpcao opcao = candidato.opcao();
        if (!opcao.getFornecedor().isAtivo()) {
            motivos.add(new Motivo(SituacaoAlternativa.FORNECEDOR_DESATIVADO, "O fornecedor está desativado: "
                    + "a opção não foi calculada nem classificada."));
        }
        if (!candidato.produtoAtivo()) {
            motivos.add(new Motivo(SituacaoAlternativa.PRODUTO_DESATIVADO, "O produto está desativado: "
                    + "a opção não foi calculada nem classificada."));
        }
        CalculoCusto calculo = candidato.calculo();
        if (calculo == null) {
            return motivos;
        }
        if (calculo.getStatus() != StatusCalculo.CALCULADO) {
            String pendencias = calculo.getPendencias().stream()
                    .filter(PendenciaCalculo::isBloqueante)
                    .map(PendenciaCalculo::getMensagem)
                    .collect(Collectors.joining(" | "));
            motivos.add(new Motivo(SituacaoAlternativa.CALCULO_INCOMPLETO,
                    "O custo efetivo não pôde ser calculado: " + pendencias));
        }
        String cfop = calculo.getCfop();
        if (cfop != null && temPendencia(calculo, TipoPendencia.CFOP_NAO_PARTICIPANTE)) {
            motivos.add(new Motivo(SituacaoAlternativa.CFOP_NAO_PARTICIPANTE, "O CFOP " + cfop
                    + " da operação não está em CFOPS_PARTICIPANTES: a operação não participa da comparação."));
        }
        if (cfop != null && temPendencia(calculo, TipoPendencia.CFOPS_PARTICIPANTES_NAO_DEFINIDOS)) {
            motivos.add(new Motivo(SituacaoAlternativa.CFOPS_PARTICIPANTES_NAO_DEFINIDOS, "A operação tem o CFOP "
                    + cfop + ", mas CFOPS_PARTICIPANTES não foi definido: não é possível confirmar "
                    + "se ela participa da comparação."));
        }
        if (calculo.getStatus() == StatusCalculo.CALCULADO && (calculo.getQuantidade() == null
                || calculo.getQuantidade().compareTo(candidato.quantidadeCotacao()) != 0)) {
            motivos.add(new Motivo(SituacaoAlternativa.QUANTIDADE_DIVERGENTE, "A quantidade da operação calculada ("
                    + (calculo.getQuantidade() == null ? "não informada" : calculo.getQuantidade().stripTrailingZeros().toPlainString())
                    + ") difere da quantidade da cotação (" + candidato.quantidadeCotacao().stripTrailingZeros().toPlainString()
                    + "): opções com quantidades diferentes não são classificadas."));
        }
        return motivos;
    }

    private static boolean temPendencia(CalculoCusto calculo, TipoPendencia tipo) {
        return calculo.getPendencias().stream().anyMatch(p -> tipo.name().equals(p.getTipo()));
    }

    private static String texto(List<Motivo> motivos) {
        String texto = motivos.stream().map(Motivo::mensagem).collect(Collectors.joining(" "));
        return texto.length() <= TAMANHO_MAXIMO_MOTIVO ? texto : texto.substring(0, TAMANHO_MAXIMO_MOTIVO - 3) + "...";
    }
}
