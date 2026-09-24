package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.calculation.CandidatoComparacao;
import br.com.squadcore.comparaprecos.calculation.ComparadorAlternativas;
import br.com.squadcore.comparaprecos.dto.CalculoRequest;
import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.Comparacao;
import br.com.squadcore.comparaprecos.entity.Cotacao;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.ParametroComparacao;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.RegraComparacao;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.ResultadoComparacao;
import br.com.squadcore.comparaprecos.exception.ComparacaoNaoEncontradaException;
import br.com.squadcore.comparaprecos.exception.CotacaoNaoEncontradaException;
import br.com.squadcore.comparaprecos.repository.CalculoCustoRepository;
import br.com.squadcore.comparaprecos.repository.ComparacaoRepository;
import br.com.squadcore.comparaprecos.repository.CotacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Executa e consulta comparações de cotações (RF10, RF11, ARQUITETURA §10).
 *
 * Todas as opções de uma comparação são calculadas pelo CalculoService com o MESMO contexto
 * (parâmetros e regras ativas lidos uma única vez), e esse contexto fica registrado na comparação.
 * A ordenação fica no ComparadorAlternativas. Este serviço não contém regra tributária.
 */
@Service
public class ComparacaoService {

    private final ComparacaoRepository comparacaoRepository;
    private final CotacaoRepository cotacaoRepository;
    private final CalculoCustoRepository calculoRepository;
    private final CalculoService calculoService;
    private final ComparadorAlternativas comparador;

    public ComparacaoService(ComparacaoRepository comparacaoRepository, CotacaoRepository cotacaoRepository,
                             CalculoCustoRepository calculoRepository, CalculoService calculoService,
                             ComparadorAlternativas comparador) {
        this.comparacaoRepository = comparacaoRepository;
        this.cotacaoRepository = cotacaoRepository;
        this.calculoRepository = calculoRepository;
        this.calculoService = calculoService;
        this.comparador = comparador;
    }

    @Transactional
    public ComparacaoDetalhada executar(Long cotacaoId, Long usuarioId) {
        Cotacao cotacao = cotacaoRepository.findDetalhadaById(cotacaoId).orElseThrow(CotacaoNaoEncontradaException::new);
        return executar(cotacao, usuarioId);
    }

    /** Sempre gera uma nova comparação: as anteriores continuam no histórico. */
    @Transactional
    public ComparacaoDetalhada executar(Cotacao cotacao, Long usuarioId) {
        CalculoService.Contexto contexto = calculoService.contextoVigente();
        Produto produto = cotacao.getProduto();
        List<CandidatoComparacao> candidatos = new ArrayList<>();
        Map<Long, CalculoCusto> calculos = new LinkedHashMap<>();
        for (CotacaoOpcao opcao : cotacao.getOpcoes()) {
            CalculoCusto calculo = null;
            // Fornecedor ou produto desativado: a opção não é calculada nem classificada.
            if (opcao.getFornecedor().isAtivo() && produto.isAtivo()) {
                calculo = calculoService.calcular(requisicao(cotacao, opcao), usuarioId, contexto);
                calculos.put(calculo.getId(), calculo);
            }
            candidatos.add(new CandidatoComparacao(opcao, produto.isAtivo(), cotacao.getQuantidade(), calculo));
        }
        List<ResultadoComparacao> resultados = comparador.classificar(candidatos);
        Comparacao comparacao = comparacaoRepository.saveAndFlush(
                new Comparacao(cotacao, usuarioId, parametros(contexto), regras(contexto), resultados));
        return new ComparacaoDetalhada(comparacao, calculos);
    }

    @Transactional(readOnly = true)
    public ComparacaoDetalhada buscar(Long id) {
        Comparacao comparacao = comparacaoRepository.findDetalhadaById(id)
                .orElseThrow(ComparacaoNaoEncontradaException::new);
        List<Long> ids = comparacao.getResultados().stream()
                .map(ResultadoComparacao::getCalculoId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, CalculoCusto> calculos = ids.isEmpty() ? Map.of() : calculoRepository.findDetalhadosByIdIn(ids)
                .stream()
                .collect(Collectors.toMap(CalculoCusto::getId, Function.identity()));
        return new ComparacaoDetalhada(comparacao, calculos);
    }

    /** Histórico da cotação, da comparação mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<Comparacao> listarDaCotacao(Long cotacaoId) {
        if (!cotacaoRepository.existsById(cotacaoId)) {
            throw new CotacaoNaoEncontradaException();
        }
        return comparacaoRepository.findByCotacaoIdOrderByIdDesc(cotacaoId);
    }

    @Transactional(readOnly = true)
    public Optional<ComparacaoDetalhada> ultimaDaCotacao(Long cotacaoId) {
        return comparacaoRepository.findFirstByCotacaoIdOrderByIdDesc(cotacaoId).map(c -> buscar(c.getId()));
    }

    /**
     * Operação da opção no formato do cálculo. Com item de NF-e, fornecedor e produto vêm do item
     * (validados na inclusão da opção). Valores e dados fiscais informados são enviados sempre; o
     * CalculoService só os usa quando a fonte configurada for INFORMADO.
     */
    private static CalculoRequest requisicao(Cotacao cotacao, CotacaoOpcao opcao) {
        CalculoRequest.ValoresInformados valores = !opcao.temValores() ? null : new CalculoRequest.ValoresInformados(
                opcao.getValorProduto(), opcao.getValorIpi(), opcao.getValorFrete(), opcao.getValorSeguro(),
                opcao.getValorOutrasDespesas(), opcao.getValorDesconto());
        CalculoRequest.DadosFiscaisInformados dadosFiscais = !opcao.temDadosFiscais() ? null
                : new CalculoRequest.DadosFiscaisInformados(opcao.getOrigemMercadoria(), opcao.getCfop(),
                        opcao.getAliquotaIcms(), opcao.getAliquotaIpi(), opcao.getAliquotaPis(),
                        opcao.getAliquotaCofins());
        if (opcao.getNfeItemId() != null) {
            return new CalculoRequest(opcao.getNfeItemId(), null, null, cotacao.getQuantidade(), valores, dadosFiscais);
        }
        return new CalculoRequest(null, opcao.getFornecedor().getId(), cotacao.getProduto().getId(),
                cotacao.getQuantidade(), valores, dadosFiscais);
    }

    /** Todos os parâmetros, inclusive os não definidos (valor nulo). */
    private static List<ParametroComparacao> parametros(CalculoService.Contexto contexto) {
        return Arrays.stream(ChaveParametro.values())
                .map(chave -> new ParametroComparacao(chave.name(), contexto.parametros().get(chave)))
                .toList();
    }

    private static List<RegraComparacao> regras(CalculoService.Contexto contexto) {
        return contexto.regrasAtivas().stream()
                .sorted(Comparator.comparing(RegraTributaria::getId))
                .map(r -> new RegraComparacao(r.getId(), r.getVersao(), r.getNome(), r.getTributo().name(),
                        r.getPrioridade()))
                .toList();
    }
}
