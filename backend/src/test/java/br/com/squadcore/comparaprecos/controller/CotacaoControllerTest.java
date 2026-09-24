package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.calculation.CalculadoraCustoEfetivo;
import br.com.squadcore.comparaprecos.calculation.ComparadorAlternativas;
import br.com.squadcore.comparaprecos.calculation.SeletorRegrasTributarias;
import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.Comparacao;
import br.com.squadcore.comparaprecos.entity.Cotacao;
import br.com.squadcore.comparaprecos.entity.CotacaoOpcao;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.SituacaoAlternativa;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.CalculoCustoRepository;
import br.com.squadcore.comparaprecos.repository.ComparacaoRepository;
import br.com.squadcore.comparaprecos.repository.CotacaoRepository;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeItemRepository;
import br.com.squadcore.comparaprecos.repository.ParametroCalculoRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.RegraTributariaRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.CalculoService;
import br.com.squadcore.comparaprecos.service.ComparacaoService;
import br.com.squadcore.comparaprecos.service.CotacaoService;
import br.com.squadcore.comparaprecos.service.ExportacaoComparacaoService;
import br.com.squadcore.comparaprecos.service.TabelaComparacaoCsv;
import br.com.squadcore.comparaprecos.service.TokenService;
import br.com.squadcore.comparaprecos.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cotação e comparação pela API (RF08, RF10 a RF12, CT18 a CT24, CT35), com a configuração de
 * segurança real e os serviços reais (cálculo, comparação). Os repositórios são simulados em
 * memória; a integração com o PostgreSQL é validada à parte.
 *
 * Regras e parâmetros são DADOS DE TESTE fictícios:
 * cada teste configura o que precisa, como o ADMIN faria.
 */
@WebMvcTest(controllers = {CotacaoController.class, ComparacaoController.class})
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        CotacaoService.class, ComparacaoService.class, CalculoService.class, CalculadoraCustoEfetivo.class,
        SeletorRegrasTributarias.class, ComparadorAlternativas.class, ExportacaoComparacaoService.class,
        TabelaComparacaoCsv.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class CotacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private CalculoCustoRepository calculoRepository;

    @MockitoBean
    private NfeItemRepository nfeItemRepository;

    @MockitoBean
    private FornecedorRepository fornecedorRepository;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    @MockitoBean
    private RegraTributariaRepository regraRepository;

    @MockitoBean
    private ParametroCalculoRepository parametroRepository;

    @MockitoBean
    private CotacaoRepository cotacaoRepository;

    @MockitoBean
    private ComparacaoRepository comparacaoRepository;

    private final List<CalculoCusto> calculos = new ArrayList<>();
    private final List<Cotacao> cotacoes = new ArrayList<>();
    private final List<Comparacao> comparacoes = new ArrayList<>();
    private final List<RegraTributaria> regras = new ArrayList<>();
    private final List<NfeItem> itens = new ArrayList<>();
    private final Map<ChaveParametro, String> parametros = new EnumMap<>(ChaveParametro.class);
    private final AtomicLong sequencia = new AtomicLong();
    private final AtomicLong sequenciaOpcao = new AtomicLong();

    private Usuario admin;
    private Usuario user;
    private Fornecedor fabricanteEs;
    private Fornecedor atacadistaMg;
    private Fornecedor fabricanteGo;
    private Produto produto;
    private Produto outroProduto;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, Perfil.ADMIN);
        user = usuario(2L, Perfil.USER);
        fabricanteEs = fornecedor(10L, Uf.ES, TipoFornecedor.FABRICANTE, "30 dias");
        atacadistaMg = fornecedor(11L, Uf.MG, TipoFornecedor.ATACADISTA, null);
        fabricanteGo = fornecedor(12L, Uf.GO, TipoFornecedor.FABRICANTE, "90 dias");
        produto = produto(20L, "Produto Teste A");
        outroProduto = produto(21L, "Produto Teste B");

        // Valores iniciais das migrations V6 e V7.
        parametros.put(ChaveParametro.UF_DESTINO, "GO");
        parametros.put(ChaveParametro.FONTE_UF_ORIGEM, "EMITENTE_NFE");
        parametros.put(ChaveParametro.COMPOSICAO_BASE_CREDITOS, "VALOR_OPERACAO");
        regrasDeTeste();
        simularRepositorios();
    }

    // ---- Configuração inicial: nenhuma opção é classificada sem os parâmetros ----

    @Test
    void comAConfiguracaoInicialACotacaoECriadaMasNenhumaOpcaoEClassificada() throws Exception {
        criar(user, 2, opcao(10, "325.00", "7", "10", "30 dias"), opcao(11, "325.00", "17", "10", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.produto.nome").value("Produto Teste A"))
                .andExpect(jsonPath("$.opcoes", hasSize(2)))
                .andExpect(jsonPath("$.ultimaComparacao.totalOpcoes").value(2))
                .andExpect(jsonPath("$.ultimaComparacao.totalClassificadas").value(0))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas", hasSize(0)))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas", hasSize(2)))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[*].situacao", hasItem("CALCULO_INCOMPLETO")))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].posicao").value(nullValue()))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].custoEfetivo").value(nullValue()))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].motivo",
                        containsString("FONTE_VALORES_OPERACAO")))
                .andExpect(jsonPath("$.ultimaComparacao.configuracao.parametros[?(@.chave == 'FONTE_VALORES_OPERACAO')].valor",
                        hasItem(nullValue())));
        // O cálculo é executado e registrado mesmo incompleto.
        assertThat(calculos).hasSize(2);
    }

    // ---- CT23, CT18, CT24: criação, cálculo e ordenação ----

    @Test
    void ct23_ct18_criaCalculaEOrdenaPeloCustoEfetivoComOsValoresInformados() throws Exception {
        configurarInformado();

        criar(user, 1,
                opcao(10, "325.00", "7", "10", "30 dias"),     // 325 − 75,89 = 249,11
                opcao(11, "325.00", "17", "10", "45 dias"),    // atacadista: 325 − 84,02 = 240,98
                opcao(10, "300.00", "7", "0", null))           // menor preço nominal: 300 − 40,05 = 259,95
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.todasAsOpcoesComparadas").value(true))
                .andExpect(jsonPath("$.ultimaComparacao.criterioOrdenacao", containsString("Custo efetivo crescente")))
                .andExpect(jsonPath("$.ultimaComparacao.totalClassificadas").value(3))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas", hasSize(0)))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].posicao").value(contains(1, 2, 3)))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].custoEfetivo").value(contains(240.98, 249.11, 259.95)))
                // CT24: produto, fornecedor, preço, créditos, custo, condição de pagamento e posição.
                .andExpect(jsonPath("$.ultimaComparacao.produtoNome").value("Produto Teste A"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].fornecedor.razaoSocial").value("Fornecedor Teste 11"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].fornecedor.tipo").value("ATACADISTA"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].condicaoPagamento").value("45 dias"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].prazoPagamentoBase").value(nullValue()))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].valorOperacao").value(325.00))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].totalCreditos").value(84.02))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].statusCalculo").value("CALCULADO"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.creditos[1].regra.nome")
                        .value("IPI - fornecedor atacadista (fator 0,25)"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.creditos[1].valorSemArredondamento").value(8.125))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.operacao.quantidade").value(1))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[1].condicaoPagamento").value("30 dias"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[1].prazoPagamentoBase").value("30 dias"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[1].calculo.creditos[2].valor").value(20.64))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[2].valorOperacao").value(300.00))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[2].condicaoPagamento").value(nullValue()))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].empate").value(contains(false, false, false)));
    }

    // ---- Mesma configuração para todas as opções ----

    @Test
    void todasAsOpcoesSaoCalculadasComOMesmoContextoERegistrado() throws Exception {
        configurarInformado();
        // Se os parâmetros fossem lidos a cada opção, a segunda leitura mudaria a composição.
        when(parametroRepository.findAll())
                .thenAnswer(inv -> parametrosComoEntidades())
                .thenAnswer(inv -> {
                    parametros.put(ChaveParametro.COMPOSICAO_VALOR_OPERACAO, "VALOR_PRODUTO,FRETE");
                    return parametrosComoEntidades();
                });

        criar(user, 1, opcao(10, "325.00", "7", "10", null), opcao(11, "325.00", "17", "10", null),
                opcao(10, "100.00", "7", "10", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ultimaComparacao.totalClassificadas").value(3))
                .andExpect(jsonPath("$.ultimaComparacao.configuracao.parametros[?(@.chave == 'COMPOSICAO_VALOR_OPERACAO')].valor",
                        hasItem("VALOR_PRODUTO")))
                .andExpect(jsonPath("$.ultimaComparacao.configuracao.parametros", hasSize(ChaveParametro.values().length)))
                .andExpect(jsonPath("$.ultimaComparacao.configuracao.regrasAtivas", hasSize(4)))
                .andExpect(jsonPath("$.ultimaComparacao.configuracao.regrasAtivas[0].versao").value(0));

        verify(parametroRepository, times(1)).findAll();
        verify(regraRepository, times(1)).findByAtivaTrue();
        assertThat(calculos).extracting(CalculoCusto::getParamComposicaoValorOperacao).containsOnly("VALOR_PRODUTO");
        assertThat(calculos).extracting(CalculoCusto::getParamFonteValores).containsOnly("INFORMADO");
    }

    // ---- CT20, CT35: empate determinístico; prazo não altera o custo ----

    @Test
    void ct20_ct35_empateSegueAOrdemDeCadastroEOPrazoNaoAlteraOCusto() throws Exception {
        configurarInformado();

        criar(user, 1, opcao(10, "325.00", "7", "10", "90 dias"), opcao(10, "325.00", "7", "10", "30 dias"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].custoEfetivo").value(contains(249.11, 249.11)))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].condicaoPagamento").value(contains("90 dias", "30 dias")))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].posicao").value(contains(1, 2)))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[*].empate").value(contains(true, true)))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].opcaoId").value(1))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[1].opcaoId").value(2));
    }

    // ---- Opções que não podem ser calculadas ----

    @Test
    void opcaoIncompletaFicaForaDoRankingSemCustoZero() throws Exception {
        configurarInformado();

        criar(user, 1, opcaoSemValores(10), opcao(11, "325.00", "17", "10", null))
                .andExpect(jsonPath("$.ultimaComparacao.totalClassificadas").value(1))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].opcaoId").value(2))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].posicao").value(1))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].opcaoId").value(1))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].situacao").value("CALCULO_INCOMPLETO"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].statusCalculo").value("INCOMPLETO"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].custoEfetivo").value(nullValue()))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].posicao").value(nullValue()))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].motivo", containsString("valores")))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].calculo.pendencias[*].tipo",
                        hasItem("DADO_INDISPONIVEL")));
    }

    @Test
    void ct17_regraAusenteDeixaAOpcaoNaoClassificada() throws Exception {
        configurarInformado();

        // Fornecedor em GO (mesma UF de destino): operação interna, sem regra de ICMS configurada.
        criar(user, 1, opcao(12, "325.00", "18", "10", null), opcao(10, "325.00", "7", "10", null))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas", hasSize(1)))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].fornecedor.id").value(12))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].situacao").value("CALCULO_INCOMPLETO"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].calculo.creditos[0].situacao").value("REGRA_AUSENTE"));
    }

    @Test
    void conflitoDeRegrasDeixaAOpcaoNaoClassificada() throws Exception {
        configurarInformado();
        nova("ICMS - regra de teste em conflito", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE, "1")
                .setAbrangenciaUf(AbrangenciaUf.INTERESTADUAL);

        criar(user, 1, opcao(10, "325.00", "7", "10", null))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas", hasSize(0)))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].situacao").value("CALCULO_INCOMPLETO"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].calculo.creditos[0].situacao").value("CONFLITO_REGRAS"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].calculo.pendencias[*].tipo",
                        hasItem("CONFLITO_REGRAS")));
    }

    // ---- Fornecedor e produto desativados ----

    @Test
    void naoCriaCotacaoComFornecedorOuProdutoDesativado() throws Exception {
        fabricanteEs.setAtivo(false);
        esperar400(corpo(20, "1", opcao(10, "325.00", "7", "10", null)), "opcoes[0].fornecedorId");
        outroProduto.setAtivo(false);
        esperar400(corpo(21, "1", opcao(11, "325.00", "7", "10", null)), "produtoId");
        assertThat(cotacoes).isEmpty();
        // Nada é reativado.
        assertThat(fabricanteEs.isAtivo()).isFalse();
        assertThat(outroProduto.isAtivo()).isFalse();
    }

    @Test
    void fornecedorDesativadoDepoisDaCotacaoFicaForaDaNovaComparacaoSemSerCalculado() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", null), opcao(11, "325.00", "17", "10", null))
                .andExpect(jsonPath("$.ultimaComparacao.totalClassificadas").value(2));
        fabricanteEs.setAtivo(false);
        int calculosAntes = calculos.size();

        comparar(1L, user)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.totalClassificadas").value(1))
                .andExpect(jsonPath("$.alternativas[0].fornecedor.id").value(11))
                .andExpect(jsonPath("$.naoClassificadas[0].situacao").value("FORNECEDOR_DESATIVADO"))
                .andExpect(jsonPath("$.naoClassificadas[0].fornecedor.ativo").value(false))
                .andExpect(jsonPath("$.naoClassificadas[0].motivo", containsString("O fornecedor está desativado")))
                .andExpect(jsonPath("$.naoClassificadas[0].calculo").value(nullValue()));
        assertThat(calculos).hasSize(calculosAntes + 1);
        assertThat(fabricanteEs.isAtivo()).isFalse();
    }

    @Test
    void produtoDesativadoDepoisDaCotacaoDeixaTodasAsOpcoesForaDoRanking() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", null));
        produto.setAtivo(false);

        comparar(1L, user)
                .andExpect(jsonPath("$.alternativas", hasSize(0)))
                .andExpect(jsonPath("$.naoClassificadas[0].situacao").value("PRODUTO_DESATIVADO"))
                .andExpect(jsonPath("$.naoClassificadas[0].produtoAtivo").value(false))
                .andExpect(jsonPath("$.naoClassificadas[0].motivo", containsString("O produto está desativado")));
        // Não é possível incluir novas opções em cotação de produto desativado.
        adicionarOpcao(1L, opcao(11, "325.00", "17", "10", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.produtoId").exists());
    }

    // ---- Origem dos dados conforme configuração ----

    @Test
    void comFonteNfeItemUsaOsValoresDoItemEIgnoraOsInformados() throws Exception {
        configurarNfe();
        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "6102");
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");

        criar(user, 1, """
                {"nfeItemId": %d, "condicaoPagamento": "30 dias", "valores": {"valorProduto": 1.00}}
                """.formatted(item.getId()), opcao(11, "325.00", "17", "10", null))
                .andExpect(jsonPath("$.opcoes[0].fornecedorId").value(10))
                .andExpect(jsonPath("$.opcoes[0].nfeItemId").value(item.getId()))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas", hasSize(1)))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].valorOperacao").value(325.00))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].custoEfetivo").value(249.11))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.operacao.nfeItemId").value(item.getId()))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.parametros.fonteValoresOperacao").value("NFE_ITEM"))
                // Sem item de NF-e, a opção fica sem os dados exigidos pela fonte configurada.
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].situacao").value("CALCULO_INCOMPLETO"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].motivo", containsString("NFE_ITEM")));
    }

    @Test
    void cfopDaOperacaoPrecisaEstarEntreOsParticipantes() throws Exception {
        configurarNfe();
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");
        String opcaoNfe = "{\"nfeItemId\": " + item.getId() + "}";

        // CFOPS_PARTICIPANTES não definido: a participação não pode ser confirmada.
        criar(user, 1, opcaoNfe)
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].situacao").value("CFOPS_PARTICIPANTES_NAO_DEFINIDOS"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].custoEfetivo").value(249.11));

        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "1102");
        comparar(1L, user).andExpect(jsonPath("$.naoClassificadas[0].situacao").value("CFOP_NAO_PARTICIPANTE"));

        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "1102,6102");
        comparar(1L, user)
                .andExpect(jsonPath("$.alternativas[0].situacao").value(SituacaoAlternativa.CLASSIFICADA.name()))
                .andExpect(jsonPath("$.alternativas[0].posicao").value(1));
    }

    @Test
    void quantidadeDoItemDiferenteDaCotacaoNaoEClassificada() throws Exception {
        configurarNfe();
        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "6102");
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");

        criar(user, 5, "{\"nfeItemId\": " + item.getId() + "}")
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].situacao").value("QUANTIDADE_DIVERGENTE"))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].quantidade").value(1))
                .andExpect(jsonPath("$.ultimaComparacao.naoClassificadas[0].motivo", containsString("quantidades diferentes")));
    }

    @Test
    void dadosFiscaisDaUltimaNfeComPrecoDaCotacao() throws Exception {
        configurarInformado();
        parametros.put(ChaveParametro.FONTE_DADOS_FISCAIS, "ULTIMA_NFE_FORNECEDOR_PRODUTO");
        parametros.put(ChaveParametro.FONTE_UF_ORIGEM, "EMITENTE_NFE");
        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "6102");
        NfeItem ultimo = itemNfe(atacadistaMg, produto, "100.00", "17", "10");

        criar(user, 1, opcao(11, "325.00", null, null, "60 dias"))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].valorOperacao").value(325.00))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].custoEfetivo").value(240.98))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.operacao.nfeItemDadosFiscaisId")
                        .value(ultimo.getId()))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].calculo.pendencias[*].tipo",
                        hasItem("DADOS_FISCAIS_DE_OUTRA_NFE")));
    }

    // ---- Histórico ----

    @Test
    void novaComparacaoNaoSobrescreveAAnteriorMesmoComRegraAlterada() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", null))
                .andExpect(jsonPath("$.ultimaComparacao.alternativas[0].custoEfetivo").value(249.11));

        // ADMIN altera a regra de PIS/COFINS (nova versão).
        RegraTributaria pisCofins = regra("PIS/COFINS - crédito de teste (6,35%)");
        pisCofins.setAliquota(new BigDecimal("5"));
        ReflectionTestUtils.setField(pisCofins, "versao", 1);
        comparar(1L, admin)
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.executadoPorId").value(1))
                .andExpect(jsonPath("$.alternativas[0].custoEfetivo").value(253.50))
                .andExpect(jsonPath("$.configuracao.regrasAtivas[?(@.id == %d)].versao".formatted(pisCofins.getId()),
                        hasItem(1)));

        mockMvc.perform(get("/comparacoes/1").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executadoPorId").value(2))
                .andExpect(jsonPath("$.alternativas[0].custoEfetivo").value(249.11))
                .andExpect(jsonPath("$.alternativas[0].calculo.creditos[2].regra.versao").value(0))
                .andExpect(jsonPath("$.alternativas[0].calculo.creditos[2].aliquotaObtida").value(6.35))
                .andExpect(jsonPath("$.configuracao.regrasAtivas[?(@.id == %d)].versao".formatted(pisCofins.getId()),
                        hasItem(0)));
        mockMvc.perform(get("/cotacoes/1/comparacoes").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(2, 1)))
                .andExpect(jsonPath("$[0].totalClassificadas").value(1));
        mockMvc.perform(get("/cotacoes/1").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.ultimaComparacao.id").value(2));
        assertThat(comparacoes).hasSize(2);
    }

    // ---- Inclusão de opções e consulta ----

    @Test
    void opcaoIncluidaDepoisEntraNaProximaComparacao() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", null));

        adicionarOpcao(1L, opcao(11, "325.00", "17", "10", "  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.fornecedorRazaoSocial").value("Fornecedor Teste 11"))
                .andExpect(jsonPath("$.condicaoPagamento").value(nullValue()))
                .andExpect(jsonPath("$.valores.valorProduto").value(325.00))
                .andExpect(jsonPath("$.dadosFiscais.aliquotaIcms").value(17));
        mockMvc.perform(get("/cotacoes/1").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.opcoes", hasSize(2)))
                .andExpect(jsonPath("$.todasAsOpcoesComparadas").value(false))
                .andExpect(jsonPath("$.ultimaComparacao.totalOpcoes").value(1));

        comparar(1L, user).andExpect(jsonPath("$.alternativas[0].opcaoId").value(2));
        mockMvc.perform(get("/cotacoes/1").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.todasAsOpcoesComparadas").value(true));
    }

    @Test
    void listaDaMaisRecenteComFiltroPorProduto() throws Exception {
        criar(user, 1, opcao(10, "325.00", "7", "10", null));
        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(21, "3", opcao(10, "50.00", "7", "10", null))
                        .replace("\"produtoId\"", "\"descricao\": \"Compra mensal\", \"produtoId\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Compra mensal"))
                .andExpect(jsonPath("$.criadoPorId").value(1));

        mockMvc.perform(get("/cotacoes").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(2, 1)))
                .andExpect(jsonPath("$[0].produtoNome").value("Produto Teste B"))
                .andExpect(jsonPath("$[0].quantidade").value(3));
        mockMvc.perform(get("/cotacoes?produtoId=20").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$[*].id").value(contains(1)));
    }

    @Test
    void recursosInexistentesRetornam404() throws Exception {
        mockMvc.perform(get("/cotacoes/999").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cotação não encontrada."));
        mockMvc.perform(get("/cotacoes/999/comparacoes").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
        comparar(999L, user).andExpect(status().isNotFound());
        adicionarOpcao(999L, opcao(10, "1.00", "7", "10", null)).andExpect(status().isNotFound());
        mockMvc.perform(get("/comparacoes/999").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Comparação não encontrada."));
    }

    // ---- Validações e erros de entrada (CT29) ----

    @Test
    void validacoesDaCotacao() throws Exception {
        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantidade": 0, "descricao": "%s", "opcoes": []}
                                """.formatted("x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.produtoId").value("O produto é obrigatório."))
                .andExpect(jsonPath("$.erros.quantidade").value("A quantidade deve ser maior que zero."))
                .andExpect(jsonPath("$.erros.descricao").exists())
                .andExpect(jsonPath("$.erros.opcoes").value("Informe pelo menos uma opção de fornecedor."));
        esperar400("{\"produtoId\": 20}", "quantidade");
        esperar400("{\"produtoId\": 20, \"quantidade\": 1}", "opcoes");
        esperar400("{\"produtoId\": 20, \"quantidade\": 1.23456, \"opcoes\": [{\"fornecedorId\": 10}]}", "quantidade");
        esperar400(corpo(999, "1", opcao(10, "1.00", "7", "10", null)), "produtoId");
        String muitas = Stream.generate(() -> "{\"fornecedorId\": 10}").limit(51).collect(Collectors.joining(","));
        esperar400("{\"produtoId\": 20, \"quantidade\": 1, \"opcoes\": [" + muitas + "]}", "opcoes");
        assertThat(cotacoes).isEmpty();
        assertThat(calculos).isEmpty();
    }

    @Test
    void validacoesDasOpcoes() throws Exception {
        NfeItem itemDeOutroProduto = itemNfe(fabricanteEs, outroProduto, "10.00", "7", "10");
        NfeItem itemSemProduto = itemNfe(fabricanteEs, null, "10.00", "7", "10");
        NfeItem itemValido = itemNfe(fabricanteEs, produto, "10.00", "7", "10");

        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"produtoId": 20, "quantidade": 1, "opcoes": [
                                  {"fornecedorId": 10, "condicaoPagamento": "%s",
                                   "valores": {"valorProduto": -1, "valorFrete": 1.234},
                                   "dadosFiscais": {"origemMercadoria": "9", "cfop": "12", "aliquotaIpi": 101}}]}
                                """.formatted("x".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros['opcoes[0].condicaoPagamento']").exists())
                .andExpect(jsonPath("$.erros['opcoes[0].valores.valorProduto']").value("O valor não pode ser negativo."))
                .andExpect(jsonPath("$.erros['opcoes[0].valores.valorFrete']").exists())
                .andExpect(jsonPath("$.erros['opcoes[0].dadosFiscais.origemMercadoria']").exists())
                .andExpect(jsonPath("$.erros['opcoes[0].dadosFiscais.cfop']").exists())
                .andExpect(jsonPath("$.erros['opcoes[0].dadosFiscais.aliquotaIpi']").exists());
        esperar400(corpo(20, "1", opcao(10, "1.00", "7", "10", null), "{}"), "opcoes[1].fornecedorId");
        esperar400(corpo(20, "1", "{\"fornecedorId\": 999}"), "opcoes[0].fornecedorId");
        esperar400(corpo(20, "1", "{\"nfeItemId\": 999}"), "opcoes[0].nfeItemId");
        esperar400(corpo(20, "1", "{\"nfeItemId\": %d}".formatted(itemDeOutroProduto.getId())), "opcoes[0].nfeItemId");
        esperar400(corpo(20, "1", "{\"nfeItemId\": %d}".formatted(itemSemProduto.getId())), "opcoes[0].nfeItemId");
        esperar400(corpo(20, "1", "{\"nfeItemId\": %d, \"fornecedorId\": 11}".formatted(itemValido.getId())),
                "opcoes[0].fornecedorId");
        esperar400("{\"produtoId\": 20, \"quantidade\": 1, \"opcoes\": [null]}", "opcoes[0]");
        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"produtoId\": \"abc\"}"))
                .andExpect(status().isBadRequest());
        assertThat(cotacoes).isEmpty();

        criar(user, 1, opcao(10, "1.00", "7", "10", null)).andExpect(status().isCreated());
        adicionarOpcao(1L, "{\"fornecedorId\": 999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.fornecedorId").exists());
        adicionarOpcao(1L, "{\"fornecedorId\": 10, \"valores\": {\"valorProduto\": -1}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros['valores.valorProduto']").exists());
        assertThat(cotacoes.getFirst().getOpcoes()).hasSize(1);
    }

    @Test
    void limiteDeOpcoesTambemValeParaInclusao() throws Exception {
        String cinquenta = Stream.generate(() -> "{\"fornecedorId\": 10}").limit(50).collect(Collectors.joining(","));
        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\": 20, \"quantidade\": 1, \"opcoes\": [" + cinquenta + "]}"))
                .andExpect(status().isCreated());

        adicionarOpcao(1L, "{\"fornecedorId\": 10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.opcoes").exists());
    }

    // ---- Acesso (CT01 a CT04) ----

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(post("/cotacoes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cotacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cotacoes/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/cotacoes/1/opcoes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/cotacoes/1/comparacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cotacoes/1/comparacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/comparacoes/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cotacoes").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        assertThat(cotacoes).isEmpty();
    }

    @Test
    void userEAdminExecutamEConsultam() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.criadoPorId").value(2));
        criar(admin, 1, opcao(10, "325.00", "7", "10", null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.criadoPorId").value(1));
        for (Usuario usuario : List.of(user, admin)) {
            adicionarOpcao(1L, opcao(11, "325.00", "17", "10", null), usuario).andExpect(status().isCreated());
            comparar(1L, usuario).andExpect(status().isCreated());
            mockMvc.perform(get("/cotacoes/1").header("Authorization", bearer(usuario))).andExpect(status().isOk());
            mockMvc.perform(get("/cotacoes/1/comparacoes").header("Authorization", bearer(usuario)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/comparacoes/1").header("Authorization", bearer(usuario))).andExpect(status().isOk());
        }
        // Cada cálculo registra quem executou.
        assertThat(calculos).extracting(CalculoCusto::getExecutadoPorId).contains(1L, 2L);
    }

    @Test
    void naoHaAlteracaoNemExclusaoPelaApi() throws Exception {
        for (String url : List.of("/cotacoes/1", "/cotacoes/1/opcoes/1", "/comparacoes/1")) {
            mockMvc.perform(delete(url).header("Authorization", bearer(admin)))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
            mockMvc.perform(put(url).header("Authorization", bearer(admin))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
        }
        mockMvc.perform(delete("/cotacoes/1").header("Authorization", bearer(admin)))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/comparacoes/1").header("Authorization", bearer(admin)))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---- Download da comparação (RF13, CT25) ----

    @Test
    void ct25_downloadDaComparacaoEmCsvComNomeETipoDoArquivo() throws Exception {
        configurarInformado();
        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(20, "1", opcao(10, "325.00", "7", "10", "30 dias"),
                                opcao(11, "325.00", "17", "10", "45 dias"), opcaoSemValores(12))
                                .replace("\"produtoId\"", "\"descricao\": \"Reposição de estoque\", \"produtoId\"")))
                .andExpect(status().isCreated());
        int calculosAntes = calculos.size();

        String csv = download(1L, user)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"comparacao-1-cotacao-1.csv\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<String> linhas = List.of(csv.split("\r\n"));
        assertThat(linhas.getFirst()).isEqualTo("\uFEFFComparação de fornecedores");
        assertThat(linhas).contains("Comparação;1", "Cotação;1", "Descrição da cotação;Reposição de estoque",
                "Produto;Produto Teste A", "Quantidade;1", "Total de opções;3", "Opções classificadas;2");
        int classificadas = linhas.indexOf("Alternativas classificadas (do menor para o maior custo efetivo)");
        assertThat(linhas.get(classificadas + 2)).startsWith("1;Não;2;Fornecedor Teste 11;Atacadista;45 dias;;1;325,00;")
                .endsWith(";84,02;240,98;");
        assertThat(linhas.get(classificadas + 3)).startsWith("2;Não;1;Fornecedor Teste 10;Fabricante;30 dias;30 dias;1;")
                .contains("ICMS: 22,75 | IPI: 32,50 | PIS/COFINS: 20,64").endsWith(";75,89;249,11;");
        int naoClassificadas = linhas.indexOf("Alternativas não classificadas (fora do ranking)");
        assertThat(naoClassificadas).isGreaterThan(classificadas);
        assertThat(linhas.get(naoClassificadas + 2)).startsWith("3;Fornecedor Teste 12;Fabricante;Cálculo incompleto;Incompleto;")
                .contains(";;;O custo efetivo não pôde ser calculado");
        assertThat(linhas).contains("FONTE_VALORES_OPERACAO;INFORMADO", "CFOPS_PARTICIPANTES;não definido");
        // O download não recalcula nem cria comparação.
        assertThat(calculos).hasSize(calculosAntes);
        assertThat(comparacoes).hasSize(1);
    }

    @Test
    void downloadPreservaEmpateEPosicaoRegistrados() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", "90 dias"), opcao(10, "325.00", "7", "10", null));

        String csv = download(1L, admin).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).contains("\r\n1;Sim;1;Fornecedor Teste 10;Fabricante;90 dias;30 dias;")
                .contains("\r\n2;Sim;2;Fornecedor Teste 10;Fabricante;;30 dias;")
                .contains("Nenhuma alternativa não classificada.");
    }

    @Test
    void downloadDeComparacaoAntigaNaoMudaComRegraFornecedorOuNovaComparacao() throws Exception {
        configurarInformado();
        criar(user, 1, opcao(10, "325.00", "7", "10", null));
        String original = download(1L, user).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Regra alterada, parâmetro alterado, fornecedor renomeado e nova comparação.
        RegraTributaria pisCofins = regra("PIS/COFINS - crédito de teste (6,35%)");
        pisCofins.setAliquota(new BigDecimal("5"));
        ReflectionTestUtils.setField(pisCofins, "versao", 1);
        parametros.put(ChaveParametro.CRITERIO_ARREDONDAMENTO, "MEIO_PARA_PAR");
        fabricanteEs.setRazaoSocial("Nome Novo do Fornecedor");
        comparar(1L, user).andExpect(jsonPath("$.alternativas[0].custoEfetivo").value(253.50));

        String depois = download(1L, user).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(depois).isEqualTo(original).contains(";249,11;").contains("Fornecedor Teste 10")
                .contains("CRITERIO_ARREDONDAMENTO;MEIO_PARA_CIMA");
        String nova = download(2L, user)
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"comparacao-2-cotacao-1.csv\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(nova).contains(";253,50;").contains("Nome Novo do Fornecedor").contains("CRITERIO_ARREDONDAMENTO;MEIO_PARA_PAR")
                .contains("PIS/COFINS - crédito de teste (6,35%);PIS/COFINS;1;0");
    }

    @Test
    void downloadComDadosOpcionaisAusentes() throws Exception {
        // Configuração inicial: nada classificado; sem descrição, condição ou prazo.
        criar(user, 1, "{\"fornecedorId\": 11}");

        String csv = download(1L, user).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).contains("Descrição da cotação;\r\n").contains("Nenhuma alternativa classificada.")
                .contains("\r\n1;Fornecedor Teste 11;Atacadista;Cálculo incompleto;Incompleto;;;;;")
                .contains("FONTE_VALORES_OPERACAO;não definido");
    }

    @Test
    void downloadExigeAutenticacaoEComparacaoExistente() throws Exception {
        mockMvc.perform(get("/comparacoes/1/download")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/comparacoes/1/download").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        download(999L, user)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Comparação não encontrada."));
        mockMvc.perform(get("/comparacoes/abc/download").header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/comparacoes/1/download").header("Authorization", bearer(admin)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void userEAdminBaixamAComparacao() throws Exception {
        criar(user, 1, opcao(10, "325.00", "7", "10", null));

        for (Usuario usuario : List.of(user, admin)) {
            download(1L, usuario)
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"));
        }
    }

    // ---- Apoio ----

    private ResultActions download(Long comparacaoId, Usuario usuario) throws Exception {
        return mockMvc.perform(get("/comparacoes/" + comparacaoId + "/download").header("Authorization", bearer(usuario)));
    }

    private void configurarNfe() {
        parametros.put(ChaveParametro.FONTE_VALORES_OPERACAO, "NFE_ITEM");
        parametros.put(ChaveParametro.FONTE_DADOS_FISCAIS, "NFE_ITEM");
        parametros.put(ChaveParametro.COMPOSICAO_VALOR_OPERACAO, "VALOR_PRODUTO");
        parametros.put(ChaveParametro.ARREDONDAMENTO_CREDITOS, "POR_CREDITO");
        parametros.put(ChaveParametro.CRITERIO_ARREDONDAMENTO, "MEIO_PARA_CIMA");
    }

    private void configurarInformado() {
        configurarNfe();
        parametros.put(ChaveParametro.FONTE_VALORES_OPERACAO, "INFORMADO");
        parametros.put(ChaveParametro.FONTE_DADOS_FISCAIS, "INFORMADO");
        parametros.put(ChaveParametro.FONTE_UF_ORIGEM, "CADASTRO_FORNECEDOR");
    }

    /** Opção com valor do produto e alíquotas informadas (sem CFOP). Nulo = não informado. */
    private static String opcao(long fornecedorId, String valor, String icms, String ipi, String condicao) {
        String fiscais = icms == null ? "" : ", \"dadosFiscais\": {\"origemMercadoria\": \"0\", \"aliquotaIcms\": "
                + icms + ", \"aliquotaIpi\": " + ipi + "}";
        String condicaoJson = condicao == null ? "" : ", \"condicaoPagamento\": \"" + condicao + "\"";
        return "{\"fornecedorId\": " + fornecedorId + condicaoJson + ", \"valores\": {\"valorProduto\": " + valor + "}"
                + fiscais + "}";
    }

    private static String opcaoSemValores(long fornecedorId) {
        return "{\"fornecedorId\": " + fornecedorId + ", \"dadosFiscais\": {\"aliquotaIcms\": 7, \"aliquotaIpi\": 10}}";
    }

    private static String corpo(long produtoId, String quantidade, String... opcoes) {
        return "{\"produtoId\": " + produtoId + ", \"quantidade\": " + quantidade + ", \"opcoes\": ["
                + String.join(",", opcoes) + "]}";
    }

    private ResultActions criar(Usuario usuario, int quantidade, String... opcoes) throws Exception {
        return mockMvc.perform(post("/cotacoes").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(produto.getId(), String.valueOf(quantidade), opcoes)));
    }

    private ResultActions comparar(Long cotacaoId, Usuario usuario) throws Exception {
        return mockMvc.perform(post("/cotacoes/" + cotacaoId + "/comparacoes").header("Authorization", bearer(usuario)));
    }

    private ResultActions adicionarOpcao(Long cotacaoId, String opcao) throws Exception {
        return adicionarOpcao(cotacaoId, opcao, user);
    }

    private ResultActions adicionarOpcao(Long cotacaoId, String opcao, Usuario usuario) throws Exception {
        return mockMvc.perform(post("/cotacoes/" + cotacaoId + "/opcoes").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(opcao));
    }

    private void esperar400(String corpo, String campo) throws Exception {
        mockMvc.perform(post("/cotacoes").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros['" + campo + "']").exists());
    }

    /** Mesmas regras da migration V6 (dados de teste). */
    private void regrasDeTeste() {
        RegraTributaria pisCofins = nova("PIS/COFINS - crédito de teste (6,35%)", Tributo.PIS_COFINS, FormaAliquota.PERCENTUAL_FIXO, "1");
        pisCofins.setAliquota(new BigDecimal("6.35"));
        nova("IPI - fornecedor fabricante", Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, "1")
                .setTipoFornecedor(TipoFornecedor.FABRICANTE);
        nova("IPI - fornecedor atacadista (fator 0,25)", Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, "0.25")
                .setTipoFornecedor(TipoFornecedor.ATACADISTA);
        nova("ICMS - operação interestadual", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE, "1")
                .setAbrangenciaUf(AbrangenciaUf.INTERESTADUAL);
        RegraTributaria porOrigem = nova("ICMS - por origem da mercadoria (teste, 5%)", Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO, "1");
        porOrigem.setAliquota(new BigDecimal("5"));
        porOrigem.setAtiva(false);
        RegraTributaria porFornecedor = nova("ICMS - por fornecedor (teste, 11%)", Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO, "1");
        porFornecedor.setAliquota(new BigDecimal("11"));
        porFornecedor.setAtiva(false);
        nova("IPI - por fornecedor (teste, crédito integral)", Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE, "1")
                .setAtiva(false);
    }

    private RegraTributaria nova(String nome, Tributo tributo, FormaAliquota forma, String fator) {
        RegraTributaria regra = new RegraTributaria(nome, tributo, forma);
        regra.setFator(new BigDecimal(fator));
        ReflectionTestUtils.setField(regra, "id", 100L + regras.size());
        regras.add(regra);
        return regra;
    }

    private RegraTributaria regra(String nome) {
        return regras.stream().filter(r -> r.getNome().equals(nome)).findFirst().orElseThrow();
    }

    /** Item de NF-e de teste: 1 unidade, CFOP 6102, origem 0, com as alíquotas informadas. */
    private NfeItem itemNfe(Fornecedor fornecedor, Produto produtoDoItem, String valor, String pIcms, String pIpi) {
        long id = sequencia.incrementAndGet();
        Nfe nfe = new Nfe("chave-teste-" + id, (int) id, 1, Instant.parse("2026-09-01T10:00:00Z"), "Venda",
                fornecedor, fornecedor.getCnpj(), fornecedor.getUf(), null, "GO");
        ReflectionTestUtils.setField(nfe, "id", id);
        NfeItem item = new NfeItem(1, produtoDoItem, "COD-" + id, null, "Item de teste", "84713012", "6102", "UN",
                BigDecimal.ONE, new BigDecimal(valor), new BigDecimal(valor));
        item.definirIcms("0", "00", null, new BigDecimal(valor), new BigDecimal(pIcms), null);
        item.definirIpi("50", new BigDecimal(valor), new BigDecimal(pIpi), null);
        nfe.adicionarItem(item);
        ReflectionTestUtils.setField(item, "id", 1000L + id);
        itens.add(item);
        return item;
    }

    private List<ParametroCalculo> parametrosComoEntidades() {
        return parametros.entrySet().stream().map(e -> new ParametroCalculo(e.getKey(), e.getValue())).toList();
    }

    private void simularRepositorios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fornecedorRepository.findByIdAndAtivoTrue(anyLong())).thenAnswer(inv -> Stream
                .of(fabricanteEs, atacadistaMg, fabricanteGo)
                .filter(f -> f.getId().equals(inv.getArgument(0)) && f.isAtivo()).findFirst());
        when(produtoRepository.findByIdAndAtivoTrue(anyLong())).thenAnswer(inv -> Stream.of(produto, outroProduto)
                .filter(p -> p.getId().equals(inv.getArgument(0)) && p.isAtivo()).findFirst());
        when(regraRepository.findByAtivaTrue()).thenAnswer(inv -> regras.stream().filter(RegraTributaria::isAtiva).toList());
        when(parametroRepository.findAll()).thenAnswer(inv -> parametrosComoEntidades());
        when(nfeItemRepository.findDetalhadoById(anyLong())).thenAnswer(inv -> itens.stream()
                .filter(i -> i.getId().equals(inv.getArgument(0))).findFirst());
        when(nfeItemRepository.findDoFornecedorEProdutoMaisRecentes(anyLong(), anyLong())).thenAnswer(inv -> itens.stream()
                .filter(i -> i.getNfe().getFornecedor().getId().equals(inv.getArgument(0))
                        && i.getProduto() != null && i.getProduto().getId().equals(inv.getArgument(1)))
                .sorted(Comparator.comparing((NfeItem i) -> i.getNfe().getDataEmissao()).reversed())
                .toList());

        when(calculoRepository.saveAndFlush(any(CalculoCusto.class))).thenAnswer(inv -> {
            CalculoCusto calculo = inv.getArgument(0);
            ReflectionTestUtils.setField(calculo, "id", (long) calculos.size() + 1);
            ReflectionTestUtils.setField(calculo, "executadoEm", Instant.now());
            calculos.add(calculo);
            return calculo;
        });
        when(calculoRepository.findDetalhadosByIdIn(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return calculos.stream().filter(c -> ids.contains(c.getId())).toList();
        });

        when(cotacaoRepository.saveAndFlush(any(Cotacao.class))).thenAnswer(inv -> {
            Cotacao cotacao = inv.getArgument(0);
            if (cotacao.getId() == null) {
                ReflectionTestUtils.setField(cotacao, "id", (long) cotacoes.size() + 1);
                ReflectionTestUtils.setField(cotacao, "criadoEm", Instant.now());
                cotacoes.add(cotacao);
            }
            for (CotacaoOpcao opcao : cotacao.getOpcoes()) {
                if (opcao.getId() == null) {
                    ReflectionTestUtils.setField(opcao, "id", sequenciaOpcao.incrementAndGet());
                    ReflectionTestUtils.setField(opcao, "criadoEm", Instant.now());
                }
            }
            return cotacao;
        });
        // Cotação já gravada (gerenciada): o flush persiste em cascata as opções novas, na própria
        // instância, como faz o JPA. A inclusão de opção usa esse caminho.
        doAnswer(inv -> {
            for (Cotacao cotacao : cotacoes) {
                for (CotacaoOpcao opcao : cotacao.getOpcoes()) {
                    if (opcao.getId() == null) {
                        ReflectionTestUtils.setField(opcao, "id", sequenciaOpcao.incrementAndGet());
                        ReflectionTestUtils.setField(opcao, "criadoEm", Instant.now());
                    }
                }
            }
            return null;
        }).when(cotacaoRepository).flush();
        when(cotacaoRepository.findDetalhadaById(anyLong())).thenAnswer(inv -> cotacoes.stream()
                .filter(c -> c.getId().equals(inv.getArgument(0))).findFirst());
        when(cotacaoRepository.findById(anyLong())).thenAnswer(inv -> cotacoes.stream()
                .filter(c -> c.getId().equals(inv.getArgument(0))).findFirst());
        when(cotacaoRepository.existsById(anyLong())).thenAnswer(inv -> cotacoes.stream()
                .anyMatch(c -> c.getId().equals(inv.getArgument(0))));
        when(cotacaoRepository.findAllByOrderByIdDesc()).thenAnswer(inv -> cotacoes.stream()
                .sorted(Comparator.comparing(Cotacao::getId).reversed()).toList());
        when(cotacaoRepository.findByProdutoIdOrderByIdDesc(anyLong())).thenAnswer(inv -> cotacoes.stream()
                .filter(c -> c.getProduto().getId().equals(inv.getArgument(0)))
                .sorted(Comparator.comparing(Cotacao::getId).reversed()).toList());

        when(comparacaoRepository.saveAndFlush(any(Comparacao.class))).thenAnswer(inv -> {
            Comparacao comparacao = inv.getArgument(0);
            ReflectionTestUtils.setField(comparacao, "id", (long) comparacoes.size() + 1);
            ReflectionTestUtils.setField(comparacao, "executadoEm", Instant.now());
            comparacoes.add(comparacao);
            return comparacao;
        });
        when(comparacaoRepository.findDetalhadaById(anyLong())).thenAnswer(inv -> comparacoes.stream()
                .filter(c -> c.getId().equals(inv.getArgument(0))).findFirst());
        when(comparacaoRepository.findByCotacaoIdOrderByIdDesc(anyLong())).thenAnswer(inv -> comparacoes.stream()
                .filter(c -> c.getCotacaoId().equals(inv.getArgument(0)))
                .sorted(Comparator.comparing(Comparacao::getId).reversed()).toList());
        when(comparacaoRepository.findFirstByCotacaoIdOrderByIdDesc(anyLong())).thenAnswer(inv -> comparacoes.stream()
                .filter(c -> c.getCotacaoId().equals(inv.getArgument(0)))
                .max(Comparator.comparing(Comparacao::getId)));
        clearInvocations(parametroRepository, regraRepository);
    }

    private static Fornecedor fornecedor(Long id, Uf uf, TipoFornecedor tipo, String prazo) {
        Fornecedor fornecedor = new Fornecedor("Fornecedor Teste " + id, "11222333000181", uf, tipo, prazo);
        ReflectionTestUtils.setField(fornecedor, "id", id);
        return fornecedor;
    }

    private static Produto produto(Long id, String nome) {
        Produto produto = new Produto(nome, null, null);
        ReflectionTestUtils.setField(produto, "id", id);
        return produto;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario).token();
    }

    private static Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario("Usuario " + id, "usuario" + id + "@teste.local", "hash-nao-utilizado", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
