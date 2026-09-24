package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.calculation.CalculadoraCustoEfetivo;
import br.com.squadcore.comparaprecos.calculation.SeletorRegrasTributarias;
import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.AbrangenciaUf;
import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.StatusCalculo;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.CalculoCustoRepository;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeItemRepository;
import br.com.squadcore.comparaprecos.repository.ParametroCalculoRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.RegraTributariaRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.CalculoService;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cálculos de custo efetivo pela API, com a configuração de segurança real.
 * Os repositórios são simulados em memória (sem banco); a integração com o PostgreSQL é validada à parte.
 *
 * As regras e os parâmetros abaixo são DADOS DE TESTE fictícios: cada teste configura o que
 * precisa, como o ADMIN faria.
 */
@WebMvcTest(controllers = CalculoController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        CalculoService.class, CalculadoraCustoEfetivo.class, SeletorRegrasTributarias.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class CalculoControllerTest {

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

    private final List<CalculoCusto> calculos = new ArrayList<>();
    private final List<RegraTributaria> regras = new ArrayList<>();
    private final List<NfeItem> itens = new ArrayList<>();
    private final Map<ChaveParametro, String> parametros = new EnumMap<>(ChaveParametro.class);
    private final AtomicLong sequencia = new AtomicLong();

    private Usuario admin;
    private Usuario user;
    private Fornecedor fabricanteEs;
    private Fornecedor atacadistaMg;
    private Produto produto;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, Perfil.ADMIN);
        user = usuario(2L, Perfil.USER);
        fabricanteEs = fornecedor(10L, Uf.ES, TipoFornecedor.FABRICANTE);
        atacadistaMg = fornecedor(11L, Uf.MG, TipoFornecedor.ATACADISTA);
        produto = new Produto("Produto Teste", null, null);
        ReflectionTestUtils.setField(produto, "id", 20L);

        // Configuração de teste (fictícia).
        parametros.put(ChaveParametro.UF_DESTINO, "GO");
        parametros.put(ChaveParametro.FONTE_UF_ORIGEM, "EMITENTE_NFE");
        parametros.put(ChaveParametro.COMPOSICAO_BASE_CREDITOS, "VALOR_OPERACAO");
        regrasDeTeste();
        simularRepositorios();
    }

    // ---- Configuração inicial: nada é escolhido pelo sistema ----

    @Test
    void comAConfiguracaoInicialOCalculoFicaIncompletoEIndicaOsParametrosPendentes() throws Exception {
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");

        calcularComItem(item, user)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INCOMPLETO"))
                .andExpect(jsonPath("$.custoEfetivo").value(nullValue()))
                .andExpect(jsonPath("$.pendencias[?(@.tipo == 'PARAMETRO_NAO_DEFINIDO')].mensagem",
                        hasItem(org.hamcrest.Matchers.containsString("FONTE_VALORES_OPERACAO"))))
                .andExpect(jsonPath("$.pendencias[?(@.tipo == 'PARAMETRO_NAO_DEFINIDO')].mensagem",
                        hasItem(org.hamcrest.Matchers.containsString("FONTE_DADOS_FISCAIS"))))
                .andExpect(jsonPath("$.pendencias[?(@.tipo == 'PARAMETRO_NAO_DEFINIDO')].mensagem",
                        hasItem(org.hamcrest.Matchers.containsString("COMPOSICAO_VALOR_OPERACAO"))))
                .andExpect(jsonPath("$.parametros.fonteValoresOperacao").value(nullValue()));
        assertThat(calculos).hasSize(1);
        assertThat(calculos.getFirst().getStatus()).isEqualTo(StatusCalculo.INCOMPLETO);
    }

    // ---- CT12 com dados de NF-e ----

    @Test
    void ct12_calculaComOsDadosDoItemDaNfeERegistraTudoOQueFoiUsado() throws Exception {
        configurarNfe();
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");
        RegraTributaria pisCofins = regra("PIS/COFINS - crédito de teste (6,35%)");

        calcularComItem(item, user)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.executadoPorId").value(2))
                .andExpect(jsonPath("$.operacao.nfeItemId").value(item.getId()))
                .andExpect(jsonPath("$.operacao.nfeItemDadosFiscaisId").value(item.getId()))
                .andExpect(jsonPath("$.operacao.tipoFornecedor").value("FABRICANTE"))
                .andExpect(jsonPath("$.operacao.ufOrigem").value("ES"))
                .andExpect(jsonPath("$.operacao.ufDestino").value("GO"))
                .andExpect(jsonPath("$.operacao.quantidade").value(1))
                .andExpect(jsonPath("$.parametros.fonteValoresOperacao").value("NFE_ITEM"))
                .andExpect(jsonPath("$.parametros.composicaoValorOperacao").value("VALOR_PRODUTO"))
                .andExpect(jsonPath("$.parametros.composicaoBaseCreditos").value("VALOR_OPERACAO"))
                .andExpect(jsonPath("$.componentes.valorProduto").value(325.00))
                .andExpect(jsonPath("$.aliquotasOperacao.icms").value(7))
                .andExpect(jsonPath("$.aliquotasOperacao.ipi").value(10))
                .andExpect(jsonPath("$.valorOperacao").value(325.00))
                .andExpect(jsonPath("$.baseCreditos").value(325.00))
                .andExpect(jsonPath("$.creditos", hasSize(3)))
                .andExpect(jsonPath("$.creditos[0].tributo").value("ICMS"))
                .andExpect(jsonPath("$.creditos[0].regra.nome").value("ICMS - operação interestadual"))
                .andExpect(jsonPath("$.creditos[0].regra.formaAliquota").value("ALIQUOTA_DA_NFE"))
                .andExpect(jsonPath("$.creditos[0].aliquotaObtida").value(7))
                .andExpect(jsonPath("$.creditos[0].valor").value(22.75))
                .andExpect(jsonPath("$.creditos[1].regra.nome").value("IPI - fornecedor fabricante"))
                .andExpect(jsonPath("$.creditos[1].fator").value(1))
                .andExpect(jsonPath("$.creditos[1].valor").value(32.50))
                .andExpect(jsonPath("$.creditos[2].tributo").value("PIS_COFINS"))
                .andExpect(jsonPath("$.creditos[2].regra.id").value(pisCofins.getId()))
                .andExpect(jsonPath("$.creditos[2].regra.versao").value(0))
                .andExpect(jsonPath("$.creditos[2].valorSemArredondamento").value(20.6375))
                .andExpect(jsonPath("$.creditos[2].valor").value(20.64))
                .andExpect(jsonPath("$.totalCreditosSemArredondamento").value(75.8875))
                .andExpect(jsonPath("$.totalCreditos").value(75.89))
                .andExpect(jsonPath("$.diferencaArredondamento").value(0.0025))
                .andExpect(jsonPath("$.custoEfetivo").value(249.11))
                .andExpect(jsonPath("$.pendencias[*].tipo", hasItem("CFOPS_PARTICIPANTES_NAO_DEFINIDOS")))
                .andExpect(jsonPath("$.pendencias[*].bloqueante", not(hasItem(true))));
    }

    @Test
    void ufDeOrigemPeloCadastroQuandoConfigurado() throws Exception {
        configurarNfe();
        parametros.put(ChaveParametro.FONTE_UF_ORIGEM, "CADASTRO_FORNECEDOR");
        // Emitente em GO no XML, mas o cadastro diz ES: vale o cadastro.
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");
        ReflectionTestUtils.setField(item.getNfe(), "emitenteUf", Uf.GO);

        calcularComItem(item, admin)
                .andExpect(jsonPath("$.operacao.ufOrigem").value("ES"))
                .andExpect(jsonPath("$.creditos[0].tributo").value("ICMS"))
                .andExpect(jsonPath("$.creditos[0].situacao").value("CALCULADO"));
    }

    @Test
    void ct17_operacaoInternaSemRegraDeIcmsFicaIncompleta() throws Exception {
        configurarNfe();
        Fornecedor fabricanteGo = fornecedor(12L, Uf.GO, TipoFornecedor.FABRICANTE);
        NfeItem item = itemNfe(fabricanteGo, produto, "325.00", "18", "10");

        calcularComItem(item, admin)
                .andExpect(jsonPath("$.status").value("INCOMPLETO"))
                .andExpect(jsonPath("$.creditos[0].situacao").value("REGRA_AUSENTE"))
                .andExpect(jsonPath("$.creditos[1].valor").value(32.50))
                .andExpect(jsonPath("$.pendencias[?(@.tipo == 'REGRA_AUSENTE')]", hasSize(1)))
                .andExpect(jsonPath("$.custoEfetivo").value(nullValue()));
    }

    @Test
    void componenteAusenteNoItemEConsideradoZeroComAviso() throws Exception {
        configurarNfe();
        parametros.put(ChaveParametro.COMPOSICAO_VALOR_OPERACAO, "VALOR_PRODUTO,IPI,FRETE");
        NfeItem item = itemNfe(fabricanteEs, produto, "200.00", "7", "10");
        item.definirIpi("50", new BigDecimal("200.00"), new BigDecimal("10"), new BigDecimal("20.00"));

        calcularComItem(item, admin)
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.componentes.valorIpi").value(20.00))
                .andExpect(jsonPath("$.componentes.valorFrete").value(0))
                .andExpect(jsonPath("$.valorOperacao").value(220.00))
                .andExpect(jsonPath("$.pendencias[?(@.tipo == 'COMPONENTE_AUSENTE_NA_NFE')].mensagem",
                        hasItem(org.hamcrest.Matchers.containsString("FRETE"))));
    }

    @Test
    void cfopForaDaListaDeParticipantesGeraAviso() throws Exception {
        configurarNfe();
        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "1111");
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");

        calcularComItem(item, admin)
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.pendencias[*].tipo", hasItem("CFOP_NAO_PARTICIPANTE")));
    }

    @Test
    void itemSemProdutoEFornecedorDesativadoGeramAvisos() throws Exception {
        configurarNfe();
        fabricanteEs.setAtivo(false);
        NfeItem item = itemNfe(fabricanteEs, null, "325.00", "7", "10");

        calcularComItem(item, admin)
                .andExpect(jsonPath("$.operacao.produtoId").value(nullValue()))
                .andExpect(jsonPath("$.pendencias[*].tipo", hasItem("FORNECEDOR_DESATIVADO")))
                .andExpect(jsonPath("$.pendencias[*].tipo", hasItem("PRODUTO_NAO_VINCULADO")));
    }

    // ---- Operação informada (ex.: cotação) ----

    @Test
    void ct15_valoresEDadosFiscaisInformadosComAtacadista() throws Exception {
        configurarInformado();

        mockMvc.perform(post("/calculos").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fornecedorId": 11, "produtoId": 20, "quantidade": 3,
                                 "valores": {"valorProduto": 325.00},
                                 "dadosFiscais": {"origemMercadoria": "0", "cfop": "6102",
                                                  "aliquotaIcms": 11, "aliquotaIpi": 10}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.operacao.nfeItemId").value(nullValue()))
                .andExpect(jsonPath("$.operacao.tipoFornecedor").value("ATACADISTA"))
                .andExpect(jsonPath("$.operacao.ufOrigem").value("MG"))
                .andExpect(jsonPath("$.operacao.quantidade").value(3))
                .andExpect(jsonPath("$.creditos[0].valor").value(35.75))
                .andExpect(jsonPath("$.creditos[1].regra.nome").value("IPI - fornecedor atacadista (fator 0,25)"))
                .andExpect(jsonPath("$.creditos[1].aliquotaAplicada").value(2.5))
                .andExpect(jsonPath("$.creditos[1].valorSemArredondamento").value(8.125))
                .andExpect(jsonPath("$.creditos[1].valor").value(8.13))
                .andExpect(jsonPath("$.totalCreditos").value(64.52))
                .andExpect(jsonPath("$.custoEfetivo").value(260.48));
    }

    @Test
    void fonteInformadaSemOsDadosInformadosFicaIncompleta() throws Exception {
        configurarInformado();

        mockMvc.perform(post("/calculos").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fornecedorId": 11, "produtoId": 20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INCOMPLETO"))
                // Sem valores e sem dados fiscais; por consequência, VALOR_PRODUTO também fica indisponível.
                .andExpect(jsonPath("$.pendencias[?(@.tipo == 'DADO_INDISPONIVEL')]", hasSize(3)))
                .andExpect(jsonPath("$.custoEfetivo").value(nullValue()));
    }

    @Test
    void dadosFiscaisDaUltimaNfeDoFornecedorEProdutoRespeitandoCfopsParticipantes() throws Exception {
        configurarInformado();
        parametros.put(ChaveParametro.FONTE_DADOS_FISCAIS, "ULTIMA_NFE_FORNECEDOR_PRODUTO");
        parametros.put(ChaveParametro.FONTE_UF_ORIGEM, "EMITENTE_NFE");
        parametros.put(ChaveParametro.CFOPS_PARTICIPANTES, "6102");
        NfeItem antiga = itemNfe(atacadistaMg, produto, "100.00", "11", "10");
        NfeItem recenteNaoParticipante = itemNfe(atacadistaMg, produto, "100.00", "7", "5");
        ReflectionTestUtils.setField(recenteNaoParticipante, "cfop", "6910");
        ReflectionTestUtils.setField(recenteNaoParticipante.getNfe(), "dataEmissao", Instant.parse("2026-09-20T10:00:00Z"));

        mockMvc.perform(post("/calculos").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fornecedorId": 11, "produtoId": 20, "valores": {"valorProduto": 325.00}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operacao.nfeItemDadosFiscaisId").value(antiga.getId()))
                .andExpect(jsonPath("$.operacao.cfop").value("6102"))
                .andExpect(jsonPath("$.aliquotasOperacao.ipi").value(10))
                .andExpect(jsonPath("$.pendencias[*].tipo", hasItem("DADOS_FISCAIS_DE_OUTRA_NFE")));
    }

    // ---- Histórico: alteração posterior da regra não altera o cálculo ----

    @Test
    void alteracaoPosteriorDaRegraNaoApagaORegistroDoCalculo() throws Exception {
        configurarNfe();
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");
        RegraTributaria pisCofins = regra("PIS/COFINS - crédito de teste (6,35%)");
        calcularComItem(item, admin).andExpect(jsonPath("$.custoEfetivo").value(249.11));
        Long primeiro = calculos.getFirst().getId();

        // ADMIN altera a alíquota (nova versão) e depois desativa a regra.
        pisCofins.setAliquota(new BigDecimal("5"));
        ReflectionTestUtils.setField(pisCofins, "versao", 1);
        calcularComItem(item, admin)
                .andExpect(jsonPath("$.creditos[2].regra.versao").value(1))
                .andExpect(jsonPath("$.creditos[2].aliquotaObtida").value(5));
        pisCofins.setAtiva(false);

        mockMvc.perform(get("/calculos/" + primeiro).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditos[2].regra.id").value(pisCofins.getId()))
                .andExpect(jsonPath("$.creditos[2].regra.versao").value(0))
                .andExpect(jsonPath("$.creditos[2].aliquotaObtida").value(6.35))
                .andExpect(jsonPath("$.creditos[2].valor").value(20.64))
                .andExpect(jsonPath("$.custoEfetivo").value(249.11));
    }

    // ---- Consulta ----

    @Test
    void listaDoMaisRecenteComFiltrosEBuscaPorId() throws Exception {
        configurarNfe();
        calcularComItem(itemNfe(fabricanteEs, produto, "325.00", "7", "10"), admin);
        calcularComItem(itemNfe(fabricanteEs, null, "100.00", "7", "10"), admin);

        mockMvc.perform(get("/calculos").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].valorOperacao").value(100.00));
        mockMvc.perform(get("/calculos?produtoId=20").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].custoEfetivo").value(249.11));
        mockMvc.perform(get("/calculos?fornecedorId=999").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/calculos/999").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cálculo não encontrado."));
    }

    // ---- Validações ----

    @Test
    void validacoesDaOperacao() throws Exception {
        NfeItem item = itemNfe(fabricanteEs, produto, "325.00", "7", "10");

        esperar400(("{\"nfeItemId\": %d, \"fornecedorId\": 10}").formatted(item.getId()), "nfeItemId");
        esperar400("{}", "fornecedorId");
        esperar400("{\"fornecedorId\": 10}", "produtoId");
        esperar400("{\"nfeItemId\": 999}", "nfeItemId");
        esperar400("{\"fornecedorId\": 999, \"produtoId\": 20}", "fornecedorId");
        esperar400("{\"fornecedorId\": 10, \"produtoId\": 999}", "produtoId");
        assertThat(calculos).isEmpty();
    }

    @Test
    void validacoesDosValoresInformados() throws Exception {
        mockMvc.perform(post("/calculos").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fornecedorId": 10, "produtoId": 20, "quantidade": 0,
                                 "valores": {"valorProduto": -1, "valorFrete": 1.234},
                                 "dadosFiscais": {"origemMercadoria": "9", "cfop": "12", "aliquotaIcms": 101}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.quantidade").exists())
                .andExpect(jsonPath("$.erros['valores.valorProduto']").value("O valor não pode ser negativo."))
                .andExpect(jsonPath("$.erros['valores.valorFrete']").exists())
                .andExpect(jsonPath("$.erros['dadosFiscais.origemMercadoria']").exists())
                .andExpect(jsonPath("$.erros['dadosFiscais.cfop']").exists())
                .andExpect(jsonPath("$.erros['dadosFiscais.aliquotaIcms']").exists());
        assertThat(calculos).isEmpty();
    }

    // ---- Acesso (CT01 a CT04) ----

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(post("/calculos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/calculos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/calculos/1")).andExpect(status().isUnauthorized());
        assertThat(calculos).isEmpty();
    }

    @Test
    void naoHaAlteracaoNemExclusaoDeCalculoPelaApi() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/calculos/1")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/calculos/1")
                        .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---- Apoio ----

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

    private ResultActions calcularComItem(NfeItem item, Usuario usuario) throws Exception {
        return mockMvc.perform(post("/calculos").header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nfeItemId\": " + item.getId() + "}"));
    }

    private void esperar400(String corpo, String campo) throws Exception {
        mockMvc.perform(post("/calculos").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros." + campo).exists());
    }

    /** Regras fictícias de teste: quatro ativas e três desativadas. */
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

    private void simularRepositorios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fornecedorRepository.findByIdAndAtivoTrue(anyLong())).thenAnswer(inv -> java.util.stream.Stream
                .of(fabricanteEs, atacadistaMg)
                .filter(f -> f.getId().equals(inv.getArgument(0)) && f.isAtivo()).findFirst());
        when(produtoRepository.findByIdAndAtivoTrue(anyLong())).thenAnswer(inv ->
                produto.getId().equals(inv.getArgument(0)) ? Optional.of(produto) : Optional.empty());
        when(regraRepository.findByAtivaTrue()).thenAnswer(inv -> regras.stream().filter(RegraTributaria::isAtiva).toList());
        when(parametroRepository.findAll()).thenAnswer(inv -> parametros.entrySet().stream()
                .map(e -> new ParametroCalculo(e.getKey(), e.getValue())).toList());
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
        when(calculoRepository.findDetalhadoById(anyLong())).thenAnswer(inv -> calculos.stream()
                .filter(c -> c.getId().equals(inv.getArgument(0))).findFirst());
        when(calculoRepository.findAllByOrderByIdDesc()).thenAnswer(inv -> calculos.stream()
                .sorted(Comparator.comparing(CalculoCusto::getId).reversed()).toList());
    }

    private static Fornecedor fornecedor(Long id, Uf uf, TipoFornecedor tipo) {
        Fornecedor fornecedor = new Fornecedor("Fornecedor Teste " + id, "11222333000181", uf, tipo, "30 dias");
        ReflectionTestUtils.setField(fornecedor, "id", id);
        return fornecedor;
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
