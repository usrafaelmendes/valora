package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.calculation.SeletorRegrasTributarias;
import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.FormaAliquota;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.ParametroCalculoRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.RegraTributariaRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.ParametroCalculoService;
import br.com.squadcore.comparaprecos.service.RegraTributariaService;
import br.com.squadcore.comparaprecos.service.TokenService;
import br.com.squadcore.comparaprecos.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regras tributárias configuráveis com a configuração de segurança real.
 * Os repositórios são simulados em memória (sem banco); a integração com o PostgreSQL é validada à parte.
 * Alíquotas, nomes e cadastros abaixo são dados de teste.
 */
@WebMvcTest(controllers = RegraTributariaController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        RegraTributariaService.class, ParametroCalculoService.class, SeletorRegrasTributarias.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class RegraTributariaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private RegraTributariaRepository regraRepository;

    @MockitoBean
    private FornecedorRepository fornecedorRepository;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    @MockitoBean
    private ParametroCalculoRepository parametroRepository;

    private final List<RegraTributaria> banco = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong();

    private Usuario admin;
    private Usuario user;
    private Fornecedor fornecedorAtivo;
    private Produto produtoAtivo;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, "Admin Teste", "admin@teste.local", Perfil.ADMIN);
        user = usuario(2L, "User Teste", "user@teste.local", Perfil.USER);
        fornecedorAtivo = new Fornecedor("Fornecedor Teste", "11222333000181", Uf.ES, TipoFornecedor.ATACADISTA, null);
        ReflectionTestUtils.setField(fornecedorAtivo, "id", 10L);
        produtoAtivo = new Produto("Produto Teste", null, null);
        ReflectionTestUtils.setField(produtoAtivo, "id", 20L);
        simularRepositoriosEmMemoria();
    }

    // Cadastro

    @Test
    void adminCadastraRegraComPercentualFixo() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "  ICMS teste  ", "tributo": "ICMS", "formaAliquota": "PERCENTUAL_FIXO",
                                 "aliquota": 4.5, "origensMercadoria": ["2", "1"], "cfops": ["2222"],
                                 "observacao": "  dado de teste  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("ICMS teste"))
                .andExpect(jsonPath("$.observacao").value("dado de teste"))
                .andExpect(jsonPath("$.tributo").value("ICMS"))
                .andExpect(jsonPath("$.aliquota").value(4.5))
                .andExpect(jsonPath("$.fator").value(1))
                .andExpect(jsonPath("$.prioridade").value(0))
                .andExpect(jsonPath("$.ativa").value(true))
                .andExpect(jsonPath("$.origensMercadoria[0]").value("1"))
                .andExpect(jsonPath("$.origensMercadoria[1]").value("2"))
                .andExpect(jsonPath("$.cfops[0]").value("2222"));
        assertThat(banco).hasSize(1);
    }

    @Test
    void adminCadastraRegraComAliquotaDaNfeFatorECondicoes() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "IPI teste", "tributo": "IPI", "formaAliquota": "ALIQUOTA_DA_NFE",
                                 "fator": 0.25, "prioridade": 5, "ativa": false, "tipoFornecedor": "ATACADISTA",
                                 "fornecedorId": 10, "produtoId": 20, "ufOrigem": "ES", "ufDestino": "GO",
                                 "abrangenciaUf": "INTERESTADUAL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aliquota").value(nullValue()))
                .andExpect(jsonPath("$.fator").value(0.25))
                .andExpect(jsonPath("$.prioridade").value(5))
                .andExpect(jsonPath("$.ativa").value(false))
                .andExpect(jsonPath("$.tipoFornecedor").value("ATACADISTA"))
                .andExpect(jsonPath("$.fornecedorId").value(10))
                .andExpect(jsonPath("$.produtoId").value(20))
                .andExpect(jsonPath("$.ufOrigem").value("ES"))
                .andExpect(jsonPath("$.ufDestino").value("GO"))
                .andExpect(jsonPath("$.abrangenciaUf").value("INTERESTADUAL"))
                .andExpect(jsonPath("$.origensMercadoria", hasSize(0)));
    }

    @Test
    void ct13_regraSemCreditoNaoTemAliquotaNemFator() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Sem crédito teste", "IPI", "SEM_CREDITO", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aliquota").value(nullValue()))
                .andExpect(jsonPath("$.fator").value(nullValue()));
    }

    // Validação

    @Test
    void rejeitaCamposObrigatoriosAusentes() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").value("O nome é obrigatório."))
                .andExpect(jsonPath("$.erros.tributo").value("O tributo é obrigatório."))
                .andExpect(jsonPath("$.erros.formaAliquota").exists());
        verify(regraRepository, never()).saveAndFlush(any());
    }

    @Test
    void percentualFixoExigeAliquota() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Regra", "ICMS", "PERCENTUAL_FIXO", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.aliquota").value("A alíquota é obrigatória para PERCENTUAL_FIXO."));
        assertThat(banco).isEmpty();
    }

    @Test
    void aliquotaSoPodeSerInformadaParaPercentualFixo() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Regra", "ICMS", "ALIQUOTA_DA_NFE", "11")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.aliquota").exists());
    }

    @Test
    void semCreditoNaoAceitaFator() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "IPI", "formaAliquota": "SEM_CREDITO", "fator": 0.25}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.fator").value("O fator não se aplica a SEM_CREDITO."));
    }

    @Test
    void rejeitaAliquotaForaDaFaixaFatorNaoPositivoEPrioridadeInvalida() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "ICMS", "formaAliquota": "PERCENTUAL_FIXO",
                                 "aliquota": 100.5, "fator": 0, "prioridade": 1001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.aliquota").exists())
                .andExpect(jsonPath("$.erros.fator").value("O fator deve ser maior que zero."))
                .andExpect(jsonPath("$.erros.prioridade").exists());
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "ICMS", "formaAliquota": "PERCENTUAL_FIXO",
                                 "aliquota": 4.12345}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.aliquota").value("A alíquota aceita no máximo 4 casas decimais."));
    }

    @Test
    void rejeitaOrigemECfopInvalidos() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "ICMS", "formaAliquota": "ALIQUOTA_DA_NFE",
                                 "origensMercadoria": ["9"], "cfops": ["12"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros['origensMercadoria[]']").exists())
                .andExpect(jsonPath("$.erros['cfops[]']").exists());
    }

    @Test
    void rejeitaValorForaDoEnum() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Regra", "ISS", "PERCENTUAL_FIXO", "5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.tributo")
                        .value("Valor inválido. Valores aceitos: ICMS, IPI, PIS, COFINS, PIS_COFINS."));
    }

    @Test
    void rejeitaAbrangenciaContraditoriaComAsUfs() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "ICMS", "formaAliquota": "ALIQUOTA_DA_NFE",
                                 "ufOrigem": "GO", "ufDestino": "GO", "abrangenciaUf": "INTERESTADUAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.abrangenciaUf").exists());
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "ICMS", "formaAliquota": "ALIQUOTA_DA_NFE",
                                 "ufOrigem": "ES", "ufDestino": "GO", "abrangenciaUf": "INTERNA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.abrangenciaUf").exists());
        assertThat(banco).isEmpty();
    }

    @Test
    void rejeitaFornecedorOuProdutoInexistenteOuDesativado() throws Exception {
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "IPI", "formaAliquota": "ALIQUOTA_DA_NFE", "fornecedorId": 99}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.fornecedorId").value("Fornecedor não encontrado."));
        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Regra", "tributo": "IPI", "formaAliquota": "ALIQUOTA_DA_NFE", "produtoId": 99}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.produtoId").value("Produto não encontrado."));
        assertThat(banco).isEmpty();
    }

    @Test
    void rejeitaNomeDuplicadoSemDiferenciarMaiusculas() throws Exception {
        existente("ICMS Teste", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE).setAtiva(false);

        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(" icms teste ", "ICMS", "PERCENTUAL_FIXO", "3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma regra tributária cadastrada com este nome."));
        assertThat(banco).hasSize(1);
    }

    @Test
    void cadastroSimultaneoQueViolaIndiceUnicoRetorna409() throws Exception {
        when(regraRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_regra_tributaria_nome\""));

        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Concorrente", "ICMS", "PERCENTUAL_FIXO", "3")))
                .andExpect(status().isConflict());
    }

    // Consulta

    @Test
    void listaIncluiDesativadasEAceitaFiltros() throws Exception {
        existente("IPI Teste", Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE);
        existente("ICMS Teste", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE).setAtiva(false);

        mockMvc.perform(get("/regras-tributarias").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tributo").value("ICMS"));
        mockMvc.perform(get("/regras-tributarias?ativa=true").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("IPI Teste"));
        mockMvc.perform(get("/regras-tributarias?tributo=ICMS").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ativa").value(false));
    }

    @Test
    void buscaPorIdE404ParaInexistente() throws Exception {
        RegraTributaria regra = existente("ICMS Teste", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);

        mockMvc.perform(get("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("ICMS Teste"));
        mockMvc.perform(get("/regras-tributarias/999").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Regra tributária não encontrada."));
    }

    // Alteração, ativação e desativação

    @Test
    void adminAlteraValorECondicoesSemMudarCodigo() throws Exception {
        RegraTributaria regra = existente("ICMS Teste", Tributo.ICMS, FormaAliquota.PERCENTUAL_FIXO);
        regra.setAliquota(new BigDecimal("3"));
        regra.definirOrigensMercadoria(java.util.Set.of("1"));

        mockMvc.perform(put("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "ICMS Teste", "tributo": "ICMS", "formaAliquota": "ALIQUOTA_DA_NFE",
                                 "origensMercadoria": ["2"], "prioridade": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formaAliquota").value("ALIQUOTA_DA_NFE"))
                .andExpect(jsonPath("$.aliquota").value(nullValue()))
                .andExpect(jsonPath("$.origensMercadoria[0]").value("2"))
                .andExpect(jsonPath("$.prioridade").value(3));
        assertThat(regra.getOrigensMercadoria()).containsExactly("2");
    }

    @Test
    void alteracaoSemInformarAtivaMantemSituacaoAtual() throws Exception {
        RegraTributaria regra = existente("ICMS Teste", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        regra.setAtiva(false);

        mockMvc.perform(put("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ICMS Teste", "ICMS", "ALIQUOTA_DA_NFE", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mockMvc.perform(put("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "ICMS Teste", "tributo": "ICMS", "formaAliquota": "ALIQUOTA_DA_NFE", "ativa": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
        assertThat(regra.isAtiva()).isTrue();
    }

    @Test
    void alteracaoNaoPodeUsarNomeDeOutraRegra() throws Exception {
        existente("Primeira", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        RegraTributaria segunda = existente("Segunda", Tributo.IPI, FormaAliquota.ALIQUOTA_DA_NFE);

        mockMvc.perform(put("/regras-tributarias/" + segunda.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PRIMEIRA", "IPI", "ALIQUOTA_DA_NFE", null)))
                .andExpect(status().isConflict());
        assertThat(segunda.getNome()).isEqualTo("Segunda");
    }

    @Test
    void alterarRegraInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/regras-tributarias/999").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Regra", "ICMS", "ALIQUOTA_DA_NFE", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDesativaRegraSemApagarORegistro() throws Exception {
        RegraTributaria regra = existente("ICMS Teste", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);

        mockMvc.perform(delete("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(banco).containsExactly(regra);
        assertThat(regra.isAtiva()).isFalse();
        verify(regraRepository, never()).delete(any());
        verify(regraRepository, never()).deleteById(anyLong());
        mockMvc.perform(delete("/regras-tributarias/999").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    // Conferência das regras aplicáveis

    @Test
    void aplicaveisUsaUfDestinoDoParametroQuandoNaoInformada() throws Exception {
        RegraTributaria interestadual = existente("ICMS interestadual", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        interestadual.setAbrangenciaUf(br.com.squadcore.comparaprecos.entity.AbrangenciaUf.INTERESTADUAL);
        RegraTributaria pisCofins = existente("PIS/COFINS", Tributo.PIS_COFINS, FormaAliquota.PERCENTUAL_FIXO);
        pisCofins.setAliquota(new BigDecimal("6.35"));
        existente("Desativada", Tributo.IPI, FormaAliquota.PERCENTUAL_FIXO).setAtiva(false);

        mockMvc.perform(post("/regras-tributarias/aplicaveis").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ufOrigem": "ES", "origemMercadoria": "2", "cfop": "6910"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operacao.ufDestino").value("GO"))
                .andExpect(jsonPath("$.tributos", hasSize(4)))
                .andExpect(jsonPath("$.tributos[0].tributo").value("ICMS"))
                .andExpect(jsonPath("$.tributos[0].situacao").value("APLICAVEL"))
                .andExpect(jsonPath("$.tributos[0].regras[0].nome").value("ICMS interestadual"))
                .andExpect(jsonPath("$.tributos[1].tributo").value("IPI"))
                .andExpect(jsonPath("$.tributos[1].situacao").value("SEM_REGRA"))
                .andExpect(jsonPath("$.tributos[1].mensagem").exists())
                .andExpect(jsonPath("$.tributos[2].regras[0].aliquota").value(6.35))
                .andExpect(jsonPath("$.tributos[3].regras[0].nome").value("PIS/COFINS"));
    }

    @Test
    void aplicaveisInformaConflitoDePrioridade() throws Exception {
        existente("ICMS A", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);
        existente("ICMS B", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);

        mockMvc.perform(post("/regras-tributarias/aplicaveis").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tributos[0].situacao").value("CONFLITO"))
                .andExpect(jsonPath("$.tributos[0].regras", hasSize(2)));
    }

    @Test
    void aplicaveisValidaOsCampos() throws Exception {
        mockMvc.perform(post("/regras-tributarias/aplicaveis").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origemMercadoria": "X", "cfop": "1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.origemMercadoria").exists())
                .andExpect(jsonPath("$.erros.cfop").exists());
    }

    // Acesso: USER consulta; alterações exigem ADMIN (CT03, CT04)

    @Test
    void ct04_userNaoCriaAlteraDesativaNemConfereRegras() throws Exception {
        RegraTributaria regra = existente("ICMS Teste", Tributo.ICMS, FormaAliquota.ALIQUOTA_DA_NFE);

        mockMvc.perform(post("/regras-tributarias").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nova", "ICMS", "PERCENTUAL_FIXO", "3")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
        mockMvc.perform(put("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Alterada", "ICMS", "PERCENTUAL_FIXO", "3")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/regras-tributarias/" + regra.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/regras-tributarias/aplicaveis").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        assertThat(banco).containsExactly(regra);
        assertThat(regra.getNome()).isEqualTo("ICMS Teste");
        assertThat(regra.isAtiva()).isTrue();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/regras-tributarias")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/regras-tributarias/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/regras-tributarias").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nova", "ICMS", "PERCENTUAL_FIXO", "3")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/regras-tributarias/1")).andExpect(status().isUnauthorized());
        assertThat(banco).isEmpty();
    }

    /** Repositórios em memória com o comportamento dos métodos usados pelos services. */
    private void simularRepositoriosEmMemoria() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fornecedorRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(fornecedorAtivo));
        when(produtoRepository.findByIdAndAtivoTrue(20L)).thenReturn(Optional.of(produtoAtivo));
        when(parametroRepository.findById(ChaveParametro.UF_DESTINO))
                .thenReturn(Optional.of(new ParametroCalculo(ChaveParametro.UF_DESTINO, "GO")));

        when(regraRepository.saveAndFlush(any(RegraTributaria.class))).thenAnswer(inv -> gravar(inv.getArgument(0)));
        when(regraRepository.findById(anyLong())).thenAnswer(inv -> banco.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst());
        when(regraRepository.findByNomeIgnoreCase(anyString())).thenAnswer(inv -> banco.stream()
                .filter(r -> r.getNome().equalsIgnoreCase(inv.getArgument(0))).findFirst());
        when(regraRepository.findByAtivaTrue()).thenAnswer(inv -> banco.stream()
                .filter(RegraTributaria::isAtiva).toList());
        when(regraRepository.findAllByOrderByTributoAscPrioridadeDescNomeAscIdAsc()).thenAnswer(inv -> banco.stream()
                .sorted(Comparator.comparing(RegraTributaria::getTributo)
                        .thenComparing(RegraTributaria::getPrioridade, Comparator.reverseOrder())
                        .thenComparing(RegraTributaria::getNome))
                .toList());
    }

    private RegraTributaria gravar(RegraTributaria regra) {
        if (regra.getId() == null) {
            ReflectionTestUtils.setField(regra, "id", sequencia.incrementAndGet());
            banco.add(regra);
        }
        return regra;
    }

    private RegraTributaria existente(String nome, Tributo tributo, FormaAliquota forma) {
        RegraTributaria regra = new RegraTributaria(nome, tributo, forma);
        regra.setFator(forma == FormaAliquota.SEM_CREDITO ? null : BigDecimal.ONE);
        return gravar(regra);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario).token();
    }

    private static String json(String nome, String tributo, String forma, String aliquota) {
        return """
                {"nome": "%s", "tributo": "%s", "formaAliquota": "%s", "aliquota": %s}
                """.formatted(nome, tributo, forma, aliquota == null ? "null" : aliquota);
    }

    private static Usuario usuario(Long id, String nome, String email, Perfil perfil) {
        Usuario usuario = new Usuario(nome, email, "hash-nao-utilizado", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
