package br.com.squadcore.comparaprecos.calculation;

import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Tributo;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Escolhe, entre as regras configuradas, qual vale para cada tributo em uma operação.
 * Não calcula crédito nem custo: somente a seleção, para que o cálculo saiba quais regras
 * (id e versão) usar e registrar.
 *
 * Critérios (mecanismo técnico; os valores de prioridade são configuração do ADMIN):
 * 1. considera somente regras ativas cujas condições preenchidas são todas atendidas;
 * 2. entre elas, vence a de maior prioridade;
 * 3. empate na maior prioridade é CONFLITO: nenhuma regra é escolhida, pois o sistema não
 *    decide sozinho qual regra configurada prevalece;
 * 4. nenhuma regra aplicável é SEM_REGRA: o sistema não inventa crédito (CT17).
 *
 * PIS e COFINS: uma regra PIS_COFINS (crédito combinado, REGRAS §4) concorre com as regras
 * PIS e COFINS separadas. Quando ela for a escolhida para os dois tributos, o cálculo deve
 * aplicar a alíquota combinada uma única vez.
 */
@Component
public class SeletorRegrasTributarias {

    /** Tributos apresentados no resultado, na ordem de REGRAS_TRIBUTARIAS §10. */
    private static final List<Tributo> TRIBUTOS = List.of(Tributo.ICMS, Tributo.IPI, Tributo.PIS, Tributo.COFINS);

    public List<SelecaoTributo> selecionar(OperacaoTributavel operacao, List<RegraTributaria> regras) {
        List<RegraTributaria> aplicaveis = regras.stream()
                .filter(RegraTributaria::isAtiva)
                .filter(regra -> atende(regra, operacao))
                .toList();
        return TRIBUTOS.stream().map(tributo -> selecionar(tributo, aplicaveis)).toList();
    }

    private static SelecaoTributo selecionar(Tributo tributo, List<RegraTributaria> aplicaveis) {
        List<RegraTributaria> candidatas = aplicaveis.stream()
                .filter(regra -> cobre(regra.getTributo(), tributo))
                .toList();
        if (candidatas.isEmpty()) {
            return new SelecaoTributo(tributo, SelecaoTributo.Situacao.SEM_REGRA, List.of());
        }
        int maiorPrioridade = candidatas.stream().mapToInt(RegraTributaria::getPrioridade).max().orElseThrow();
        List<RegraTributaria> vencedoras = candidatas.stream()
                .filter(regra -> regra.getPrioridade() == maiorPrioridade)
                .sorted(Comparator.comparing(RegraTributaria::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        SelecaoTributo.Situacao situacao = vencedoras.size() == 1
                ? SelecaoTributo.Situacao.APLICAVEL
                : SelecaoTributo.Situacao.CONFLITO;
        return new SelecaoTributo(tributo, situacao, vencedoras);
    }

    private static boolean cobre(Tributo daRegra, Tributo tributo) {
        return daRegra == tributo
                || (daRegra == Tributo.PIS_COFINS && (tributo == Tributo.PIS || tributo == Tributo.COFINS));
    }

    static boolean atende(RegraTributaria regra, OperacaoTributavel operacao) {
        return condicao(regra.getTipoFornecedor(), operacao.tipoFornecedor())
                && condicao(regra.getFornecedor() == null ? null : regra.getFornecedor().getId(), operacao.fornecedorId())
                && condicao(regra.getProduto() == null ? null : regra.getProduto().getId(), operacao.produtoId())
                && condicao(regra.getUfOrigem(), operacao.ufOrigem())
                && condicao(regra.getUfDestino(), operacao.ufDestino())
                && abrangencia(regra.getAbrangenciaUf(), operacao)
                && lista(regra.getOrigensMercadoria(), operacao.origemMercadoria())
                && lista(regra.getCfops(), operacao.cfop());
    }

    /** Condição nula = qualquer valor; preenchida exige valor conhecido e igual. */
    private static boolean condicao(Object exigido, Object informado) {
        return exigido == null || Objects.equals(exigido, informado);
    }

    private static boolean lista(Set<String> aceitos, String informado) {
        return aceitos.isEmpty() || (informado != null && aceitos.contains(informado));
    }

    private static boolean abrangencia(AbrangenciaUf exigida, OperacaoTributavel operacao) {
        if (exigida == null) {
            return true;
        }
        if (operacao.ufOrigem() == null || operacao.ufDestino() == null) {
            return false;
        }
        boolean mesmaUf = operacao.ufOrigem() == operacao.ufDestino();
        return exigida == AbrangenciaUf.INTERNA ? mesmaUf : !mesmaUf;
    }
}
