package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.calculation.AliquotasOperacao;
import br.com.squadcore.comparaprecos.calculation.CalculadoraCustoEfetivo;
import br.com.squadcore.comparaprecos.calculation.ConfiguracaoCalculo;
import br.com.squadcore.comparaprecos.calculation.CreditoCalculado;
import br.com.squadcore.comparaprecos.calculation.EntradaCalculo;
import br.com.squadcore.comparaprecos.calculation.OperacaoTributavel;
import br.com.squadcore.comparaprecos.calculation.Pendencia;
import br.com.squadcore.comparaprecos.calculation.ResultadoCalculo;
import br.com.squadcore.comparaprecos.calculation.TipoPendencia;
import br.com.squadcore.comparaprecos.dto.CalculoRequest;
import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.ComponenteValor;
import br.com.squadcore.comparaprecos.entity.CreditoCalculo;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import br.com.squadcore.comparaprecos.entity.PendenciaCalculo;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.exception.CalculoNaoEncontradoException;
import br.com.squadcore.comparaprecos.exception.CampoInvalidoException;
import br.com.squadcore.comparaprecos.repository.CalculoCustoRepository;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeItemRepository;
import br.com.squadcore.comparaprecos.repository.ParametroCalculoRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.RegraTributariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Executa e consulta cálculos de custo efetivo (RF09, REGRAS_TRIBUTARIAS §4, §5 e §10).
 *
 * Obtém os dados da operação nas fontes configuradas pelo ADMIN (parâmetros), entrega-os à
 * CalculadoraCustoEfetivo e grava o resultado com uma cópia de tudo o que foi usado.
 * Parâmetro não definido ou dado ausente na fonte configurada viram pendências: o serviço
 * nunca escolhe uma fonte ou um valor por conta própria.
 */
@Service
public class CalculoService {

    private final CalculoCustoRepository calculoRepository;
    private final NfeItemRepository nfeItemRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoRepository produtoRepository;
    private final RegraTributariaRepository regraRepository;
    private final ParametroCalculoRepository parametroRepository;
    private final CalculadoraCustoEfetivo calculadora;

    public CalculoService(CalculoCustoRepository calculoRepository, NfeItemRepository nfeItemRepository,
                          FornecedorRepository fornecedorRepository, ProdutoRepository produtoRepository,
                          RegraTributariaRepository regraRepository, ParametroCalculoRepository parametroRepository,
                          CalculadoraCustoEfetivo calculadora) {
        this.calculoRepository = calculoRepository;
        this.nfeItemRepository = nfeItemRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.produtoRepository = produtoRepository;
        this.regraRepository = regraRepository;
        this.parametroRepository = parametroRepository;
        this.calculadora = calculadora;
    }

    /**
     * Parâmetros e regras ativas vigentes, lidos uma única vez. Quem calcula várias operações
     * que precisam ser comparáveis (ex.: as opções de uma comparação) usa o mesmo contexto em
     * todas, e nenhuma alteração feita pelo ADMIN durante a execução muda parte do resultado.
     */
    public record Contexto(Map<ChaveParametro, String> parametros, List<RegraTributaria> regrasAtivas) {

        public Contexto {
            parametros = parametros.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(parametros));
            regrasAtivas = List.copyOf(regrasAtivas);
        }
    }

    @Transactional(readOnly = true)
    public Contexto contextoVigente() {
        return new Contexto(parametros(), regraRepository.findByAtivaTrue());
    }

    @Transactional
    public CalculoCusto calcular(CalculoRequest request, Long usuarioId) {
        return calcular(request, usuarioId, contextoVigente());
    }

    /** Calcula com o contexto informado (parâmetros e regras), em vez de ler os vigentes. */
    @Transactional
    public CalculoCusto calcular(CalculoRequest request, Long usuarioId, Contexto contexto) {
        Map<ChaveParametro, String> parametros = contexto.parametros();
        List<Pendencia> pendencias = new ArrayList<>();

        // Operação: item de NF-e importada ou fornecedor + produto informados.
        NfeItem item = null;
        Fornecedor fornecedor;
        Produto produto;
        if (request.nfeItemId() != null) {
            if (request.fornecedorId() != null || request.produtoId() != null) {
                throw new CampoInvalidoException("nfeItemId",
                        "Informe nfeItemId ou fornecedorId e produtoId, não os dois.");
            }
            item = nfeItemRepository.findDetalhadoById(request.nfeItemId())
                    .orElseThrow(() -> new CampoInvalidoException("nfeItemId", "Item de NF-e não encontrado."));
            fornecedor = item.getNfe().getFornecedor();
            produto = item.getProduto();
            if (!fornecedor.isAtivo()) {
                pendencias.add(new Pendencia(TipoPendencia.FORNECEDOR_DESATIVADO,
                        "O fornecedor emitente da NF-e está desativado."));
            }
            if (produto == null) {
                pendencias.add(new Pendencia(TipoPendencia.PRODUTO_NAO_VINCULADO,
                        "O item da NF-e não está vinculado a um produto cadastrado: "
                                + "regras com condição por produto não se aplicam."));
            }
        } else {
            if (request.fornecedorId() == null) {
                throw new CampoInvalidoException("fornecedorId", "Informe nfeItemId ou fornecedorId e produtoId.");
            }
            if (request.produtoId() == null) {
                throw new CampoInvalidoException("produtoId", "O produto é obrigatório quando não há item de NF-e.");
            }
            fornecedor = fornecedorRepository.findByIdAndAtivoTrue(request.fornecedorId())
                    .orElseThrow(() -> new CampoInvalidoException("fornecedorId", "Fornecedor não encontrado."));
            produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                    .orElseThrow(() -> new CampoInvalidoException("produtoId", "Produto não encontrado."));
        }

        ConfiguracaoCalculo configuracao = configuracao(parametros);

        // Valores (preço) da operação.
        Map<ComponenteValor, BigDecimal> componentes = new EnumMap<>(ComponenteValor.class);
        BigDecimal quantidade = null;
        String fonteValores = parametros.get(ChaveParametro.FONTE_VALORES_OPERACAO);
        if (fonteValores == null) {
            pendencias.add(parametroNaoDefinido(ChaveParametro.FONTE_VALORES_OPERACAO, "a fonte dos valores da operação"));
        } else if ("NFE_ITEM".equals(fonteValores)) {
            if (item == null) {
                pendencias.add(indisponivel("FONTE_VALORES_OPERACAO = NFE_ITEM exige um item de NF-e (nfeItemId)."));
            } else {
                componentesDoItem(item, componentesUsados(configuracao), componentes, pendencias);
                quantidade = item.getQuantidade();
            }
        } else if (request.valores() == null) {
            pendencias.add(indisponivel("FONTE_VALORES_OPERACAO = INFORMADO exige os valores da operação (valores)."));
        } else {
            componentesInformados(request.valores(), componentes);
            quantidade = request.quantidade();
        }

        // Dados fiscais: origem, CFOP e alíquotas destacadas.
        NfeItem itemFiscal = null;
        String origem = null;
        String cfop = null;
        AliquotasOperacao aliquotas = AliquotasOperacao.NENHUMA;
        String fonteFiscal = parametros.get(ChaveParametro.FONTE_DADOS_FISCAIS);
        if (fonteFiscal == null) {
            pendencias.add(parametroNaoDefinido(ChaveParametro.FONTE_DADOS_FISCAIS, "a fonte dos dados fiscais"));
        } else if ("INFORMADO".equals(fonteFiscal)) {
            CalculoRequest.DadosFiscaisInformados informados = request.dadosFiscais();
            if (informados == null) {
                pendencias.add(indisponivel("FONTE_DADOS_FISCAIS = INFORMADO exige os dados fiscais (dadosFiscais)."));
            } else {
                origem = informados.origemMercadoria();
                cfop = informados.cfop();
                aliquotas = new AliquotasOperacao(informados.aliquotaIcms(), informados.aliquotaIpi(),
                        informados.aliquotaPis(), informados.aliquotaCofins());
            }
        } else {
            itemFiscal = "NFE_ITEM".equals(fonteFiscal)
                    ? itemFiscalDoProprioItem(item, pendencias)
                    : ultimoItemDoFornecedorEProduto(fornecedor, produto, parametros, pendencias);
            if (itemFiscal != null) {
                origem = itemFiscal.getIcmsOrigem();
                cfop = itemFiscal.getCfop();
                aliquotas = new AliquotasOperacao(itemFiscal.getIcmsAliquota(), itemFiscal.getIpiAliquota(),
                        itemFiscal.getPisAliquota(), itemFiscal.getCofinsAliquota());
                if (item == null || !Objects.equals(item.getId(), itemFiscal.getId())) {
                    pendencias.add(new Pendencia(TipoPendencia.DADOS_FISCAIS_DE_OUTRA_NFE,
                            "Dados fiscais obtidos do item " + itemFiscal.getId() + " da NF-e mais recente do "
                                    + "fornecedor e produto."));
                }
            }
        }

        Uf ufOrigem = ufOrigem(parametros, fornecedor, itemFiscal != null ? itemFiscal : item, pendencias);
        Uf ufDestino = ufDestino(parametros, pendencias);
        verificarCfop(cfop, parametros, pendencias);

        OperacaoTributavel operacao = new OperacaoTributavel(fornecedor.getId(), fornecedor.getTipo(),
                produto == null ? null : produto.getId(), ufOrigem, ufDestino, origem, cfop);
        ResultadoCalculo resultado = calculadora.calcular(new EntradaCalculo(operacao, componentes, aliquotas,
                configuracao, contexto.regrasAtivas(), pendencias));

        CalculoCusto calculo = new CalculoCusto(usuarioId, fornecedor.getId(), fornecedor.getTipo());
        calculo.definirOperacao(item == null ? null : item.getId(), itemFiscal == null ? null : itemFiscal.getId(),
                operacao.produtoId(), ufOrigem, ufDestino, origem, cfop, quantidade);
        calculo.definirParametros(fonteValores, fonteFiscal, parametros.get(ChaveParametro.FONTE_UF_ORIGEM),
                parametros.get(ChaveParametro.COMPOSICAO_VALOR_OPERACAO),
                parametros.get(ChaveParametro.COMPOSICAO_BASE_CREDITOS),
                parametros.get(ChaveParametro.ARREDONDAMENTO_CREDITOS),
                parametros.get(ChaveParametro.CRITERIO_ARREDONDAMENTO));
        calculo.definirComponentes(componentes.get(ComponenteValor.VALOR_PRODUTO), componentes.get(ComponenteValor.IPI),
                componentes.get(ComponenteValor.FRETE), componentes.get(ComponenteValor.SEGURO),
                componentes.get(ComponenteValor.OUTRAS_DESPESAS), componentes.get(ComponenteValor.DESCONTO));
        calculo.definirAliquotasOperacao(aliquotas.icms(), aliquotas.ipi(), aliquotas.pis(), aliquotas.cofins());
        calculo.definirResultado(resultado.completo() ? StatusCalculo.CALCULADO : StatusCalculo.INCOMPLETO,
                resultado.valorOperacao(), resultado.baseCreditos(), resultado.totalCreditosSemArredondamento(),
                resultado.totalCreditos(), resultado.diferencaArredondamento(), resultado.custoEfetivo(),
                resultado.creditos().stream().map(CalculoService::copia).toList(),
                resultado.pendencias().stream()
                        .map(p -> new PendenciaCalculo(p.tipo().name(), p.bloqueante(), p.mensagem()))
                        .toList());
        return calculoRepository.saveAndFlush(calculo);
    }

    @Transactional(readOnly = true)
    public CalculoCusto buscar(Long id) {
        return calculoRepository.findDetalhadoById(id).orElseThrow(CalculoNaoEncontradoException::new);
    }

    /** Filtros opcionais; do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<CalculoCusto> listar(Long fornecedorId, Long produtoId) {
        return calculoRepository.findAllByOrderByIdDesc().stream()
                .filter(c -> fornecedorId == null || fornecedorId.equals(c.getFornecedorId()))
                .filter(c -> produtoId == null || produtoId.equals(c.getProdutoId()))
                .toList();
    }

    // ---- Parâmetros ----

    private Map<ChaveParametro, String> parametros() {
        Map<ChaveParametro, String> valores = new EnumMap<>(ChaveParametro.class);
        for (ParametroCalculo parametro : parametroRepository.findAll()) {
            if (parametro.getValor() != null) {
                valores.put(parametro.getChave(), parametro.getValor());
            }
        }
        return valores;
    }

    private static ConfiguracaoCalculo configuracao(Map<ChaveParametro, String> parametros) {
        String composicaoValor = parametros.get(ChaveParametro.COMPOSICAO_VALOR_OPERACAO);
        String composicaoBase = parametros.get(ChaveParametro.COMPOSICAO_BASE_CREDITOS);
        boolean baseIgualValor = ChaveParametro.VALOR_OPERACAO.equals(composicaoBase);
        return new ConfiguracaoCalculo(
                composicaoValor == null ? null : ChaveParametro.componentesDe(composicaoValor),
                baseIgualValor,
                composicaoBase == null || baseIgualValor ? null : ChaveParametro.componentesDe(composicaoBase),
                parametros.get(ChaveParametro.ARREDONDAMENTO_CREDITOS),
                parametros.get(ChaveParametro.CRITERIO_ARREDONDAMENTO));
    }

    private static Set<ComponenteValor> componentesUsados(ConfiguracaoCalculo configuracao) {
        Set<ComponenteValor> usados = EnumSet.noneOf(ComponenteValor.class);
        if (configuracao.composicaoValorOperacao() != null) {
            usados.addAll(configuracao.composicaoValorOperacao());
        }
        if (configuracao.composicaoBaseCreditos() != null) {
            usados.addAll(configuracao.composicaoBaseCreditos());
        }
        return usados;
    }

    // ---- Fontes de dados ----

    /**
     * No XML, vFrete, vSeg, vDesc, vOutro e o grupo IPI só existem quando há valor: a ausência
     * no item significa zero. Isso é registrado como aviso para cada componente usado.
     */
    private static void componentesDoItem(NfeItem item, Set<ComponenteValor> usados,
                                          Map<ComponenteValor, BigDecimal> componentes, List<Pendencia> pendencias) {
        Map<ComponenteValor, BigDecimal> doItem = new EnumMap<>(ComponenteValor.class);
        doItem.put(ComponenteValor.VALOR_PRODUTO, item.getValorProduto());
        doItem.put(ComponenteValor.IPI, item.getIpiValor());
        doItem.put(ComponenteValor.FRETE, item.getValorFrete());
        doItem.put(ComponenteValor.SEGURO, item.getValorSeguro());
        doItem.put(ComponenteValor.OUTRAS_DESPESAS, item.getValorOutrasDespesas());
        doItem.put(ComponenteValor.DESCONTO, item.getValorDesconto());
        for (Map.Entry<ComponenteValor, BigDecimal> entrada : doItem.entrySet()) {
            if (entrada.getValue() != null) {
                componentes.put(entrada.getKey(), entrada.getValue());
            } else if (usados.contains(entrada.getKey())) {
                componentes.put(entrada.getKey(), BigDecimal.ZERO.setScale(2));
                pendencias.add(new Pendencia(TipoPendencia.COMPONENTE_AUSENTE_NA_NFE, "Componente "
                        + entrada.getKey() + " sem valor no item da NF-e: considerado R$ 0,00."));
            }
        }
    }

    private static void componentesInformados(CalculoRequest.ValoresInformados valores,
                                              Map<ComponenteValor, BigDecimal> componentes) {
        colocar(componentes, ComponenteValor.VALOR_PRODUTO, valores.valorProduto());
        colocar(componentes, ComponenteValor.IPI, valores.valorIpi());
        colocar(componentes, ComponenteValor.FRETE, valores.valorFrete());
        colocar(componentes, ComponenteValor.SEGURO, valores.valorSeguro());
        colocar(componentes, ComponenteValor.OUTRAS_DESPESAS, valores.valorOutrasDespesas());
        colocar(componentes, ComponenteValor.DESCONTO, valores.valorDesconto());
    }

    private static void colocar(Map<ComponenteValor, BigDecimal> componentes, ComponenteValor chave, BigDecimal valor) {
        if (valor != null) {
            componentes.put(chave, valor.setScale(2));
        }
    }

    private static NfeItem itemFiscalDoProprioItem(NfeItem item, List<Pendencia> pendencias) {
        if (item == null) {
            pendencias.add(indisponivel("FONTE_DADOS_FISCAIS = NFE_ITEM exige um item de NF-e (nfeItemId)."));
        }
        return item;
    }

    /** Item mais recente do fornecedor e produto; com CFOPS_PARTICIPANTES definido, só entre esses CFOPs. */
    private NfeItem ultimoItemDoFornecedorEProduto(Fornecedor fornecedor, Produto produto,
                                                  Map<ChaveParametro, String> parametros, List<Pendencia> pendencias) {
        if (produto == null) {
            pendencias.add(indisponivel("FONTE_DADOS_FISCAIS = ULTIMA_NFE_FORNECEDOR_PRODUTO exige o produto da operação."));
            return null;
        }
        Set<String> participantes = cfopsParticipantes(parametros);
        NfeItem encontrado = nfeItemRepository.findDoFornecedorEProdutoMaisRecentes(fornecedor.getId(), produto.getId())
                .stream()
                .filter(i -> participantes == null || participantes.contains(i.getCfop()))
                .findFirst()
                .orElse(null);
        if (encontrado == null) {
            pendencias.add(indisponivel("Nenhuma NF-e importada do fornecedor com este produto"
                    + (participantes == null ? "" : " e com CFOP participante") + " para obter os dados fiscais."));
        }
        return encontrado;
    }

    private static Uf ufOrigem(Map<ChaveParametro, String> parametros, Fornecedor fornecedor, NfeItem itemNfe,
                               List<Pendencia> pendencias) {
        String fonte = parametros.get(ChaveParametro.FONTE_UF_ORIGEM);
        if (fonte == null) {
            pendencias.add(parametroNaoDefinido(ChaveParametro.FONTE_UF_ORIGEM, "a UF de origem"));
            return null;
        }
        if ("CADASTRO_FORNECEDOR".equals(fonte)) {
            return fornecedor.getUf();
        }
        if (itemNfe == null) {
            pendencias.add(indisponivel("FONTE_UF_ORIGEM = EMITENTE_NFE exige uma NF-e na operação "
                    + "(item de NF-e ou dados fiscais da NF-e)."));
            return null;
        }
        Nfe nfe = itemNfe.getNfe();
        return nfe.getEmitenteUf();
    }

    private static Uf ufDestino(Map<ChaveParametro, String> parametros, List<Pendencia> pendencias) {
        String valor = parametros.get(ChaveParametro.UF_DESTINO);
        if (valor == null) {
            pendencias.add(parametroNaoDefinido(ChaveParametro.UF_DESTINO, "a UF de destino"));
            return null;
        }
        return Uf.valueOf(valor);
    }

    private static void verificarCfop(String cfop, Map<ChaveParametro, String> parametros, List<Pendencia> pendencias) {
        Set<String> participantes = cfopsParticipantes(parametros);
        if (participantes == null) {
            pendencias.add(new Pendencia(TipoPendencia.CFOPS_PARTICIPANTES_NAO_DEFINIDOS,
                    "CFOPS_PARTICIPANTES não definido: não foi verificado se a operação participa da comparação."));
        } else if (cfop != null && !participantes.contains(cfop)) {
            pendencias.add(new Pendencia(TipoPendencia.CFOP_NAO_PARTICIPANTE,
                    "O CFOP " + cfop + " não está em CFOPS_PARTICIPANTES."));
        }
    }

    private static Set<String> cfopsParticipantes(Map<ChaveParametro, String> parametros) {
        String valor = parametros.get(ChaveParametro.CFOPS_PARTICIPANTES);
        return valor == null ? null : Set.copyOf(Arrays.asList(valor.split(",")));
    }

    private static Pendencia parametroNaoDefinido(ChaveParametro chave, String descricao) {
        return new Pendencia(TipoPendencia.PARAMETRO_NAO_DEFINIDO,
                "Parâmetro " + chave + " não definido: não é possível obter " + descricao + ".");
    }

    private static Pendencia indisponivel(String mensagem) {
        return new Pendencia(TipoPendencia.DADO_INDISPONIVEL, mensagem);
    }

    /** Cópia da regra no momento do cálculo: id, versão, nome, forma e valores usados. */
    private static CreditoCalculo copia(CreditoCalculado credito) {
        String conflito = credito.regrasEmConflito().isEmpty() ? null : credito.regrasEmConflito().stream()
                .map(r -> r.getId() + ":" + r.getVersao())
                .collect(Collectors.joining(","));
        return new CreditoCalculo(credito.tributo().name(), credito.situacao().name(),
                credito.regra() == null ? null : credito.regra().getId(),
                credito.regra() == null ? null : credito.regra().getVersao(),
                credito.regra() == null ? null : credito.regra().getNome(),
                credito.regra() == null ? null : credito.regra().getFormaAliquota().name(),
                credito.aliquotaObtida(), credito.fator(), credito.aliquotaAplicada(), credito.base(),
                credito.valorSemArredondamento(), credito.valor(), conflito, credito.mensagem());
    }
}
