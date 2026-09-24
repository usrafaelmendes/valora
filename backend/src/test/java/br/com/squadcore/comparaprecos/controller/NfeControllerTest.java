package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Nfe;
import br.com.squadcore.comparaprecos.entity.NfeItem;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.NfeRepository;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.NfeService;
import br.com.squadcore.comparaprecos.service.NfeXmlParser;
import br.com.squadcore.comparaprecos.service.TokenService;
import br.com.squadcore.comparaprecos.service.UsuarioDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CHAVE;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.CNPJ_EMITENTE;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.GTIN_ITEM_1;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.bytes;
import static br.com.squadcore.comparaprecos.service.NfeXmlFixture.trocar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NF-e (CT09, CT10, CT11, CT03/CT04 aplicados ao recurso) com a configuração de segurança real.
 * Os repositórios são simulados em memória (sem banco); a integração com o PostgreSQL é validada à parte.
 * A NF-e, o fornecedor e o produto são dados de teste (fixture sintética).
 */
@WebMvcTest(controllers = NfeController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        NfeService.class, NfeXmlParser.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class NfeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private NfeRepository nfeRepository;

    @MockitoBean
    private FornecedorRepository fornecedorRepository;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    private final List<Nfe> banco = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong();

    private Usuario admin;
    private Usuario user;
    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, "Admin Teste", "admin@teste.local", Perfil.ADMIN);
        user = usuario(2L, "User Teste", "user@teste.local", Perfil.USER);
        fornecedor = new Fornecedor("Fornecedor Teste Ltda", CNPJ_EMITENTE, Uf.PR, TipoFornecedor.ATACADISTA, null);
        ReflectionTestUtils.setField(fornecedor, "id", 10L);
        simularRepositoriosEmMemoria();
    }

    // CT09 — Importação de XML válido

    @Test
    void ct09_adminImportaNfe() throws Exception {
        Produto produto = new Produto("Produto Teste X", null, GTIN_ITEM_1);
        ReflectionTestUtils.setField(produto, "id", 5L);
        when(produtoRepository.findByGtin(GTIN_ITEM_1)).thenReturn(Optional.of(produto));

        importar(admin, arquivo(bytes()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.chaveAcesso").value(CHAVE))
                .andExpect(jsonPath("$.numero").value(1234))
                .andExpect(jsonPath("$.serie").value(1))
                .andExpect(jsonPath("$.dataEmissao").value("2026-09-10T17:30:00Z"))
                .andExpect(jsonPath("$.fornecedor.id").value(10))
                .andExpect(jsonPath("$.fornecedor.razaoSocial").value("Fornecedor Teste Ltda"))
                .andExpect(jsonPath("$.emitenteCnpj").value(CNPJ_EMITENTE))
                .andExpect(jsonPath("$.emitenteUf").value("PR"))
                .andExpect(jsonPath("$.destinatarioUf").value("GO"))
                .andExpect(jsonPath("$.totais.valorTotal").value(411.10))
                .andExpect(jsonPath("$.itensSemProduto").value(2))
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.itens[0].produto.id").value(5))
                .andExpect(jsonPath("$.itens[0].produto.nome").value("Produto Teste X"))
                .andExpect(jsonPath("$.itens[0].gtin").value(GTIN_ITEM_1))
                .andExpect(jsonPath("$.itens[0].icms.origem").value("1"))
                .andExpect(jsonPath("$.itens[0].icms.aliquota").value(7.00))
                .andExpect(jsonPath("$.itens[0].ipi.aliquota").value(10))
                .andExpect(jsonPath("$.itens[1].produto").value(nullValue()))
                .andExpect(jsonPath("$.itens[1].gtin").value(nullValue()))
                .andExpect(jsonPath("$.itens[1].icms.valor").value(nullValue()))
                .andExpect(jsonPath("$.itens[2].produto").value(nullValue()))
                .andExpect(jsonPath("$.itens[2].ipi.cst").value(nullValue()));

        assertThat(banco).hasSize(1);
    }

    @Test
    void ct09_aceitaTiposDeConteudoXmlComuns() throws Exception {
        for (String tipo : List.of("text/xml", "application/xml; charset=UTF-8", "application/octet-stream")) {
            banco.clear();
            importar(admin, new MockMultipartFile("arquivo", "nota.xml", tipo, bytes()))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void ct09_nfeImportadaPodeSerConsultada() throws Exception {
        importar(admin, arquivo(bytes())).andExpect(status().isCreated());

        mockMvc.perform(get("/nfe").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].chaveAcesso").value(CHAVE))
                .andExpect(jsonPath("$[0].fornecedorId").value(10))
                .andExpect(jsonPath("$[0].fornecedorRazaoSocial").value("Fornecedor Teste Ltda"))
                .andExpect(jsonPath("$[0].valorTotal").value(411.10))
                .andExpect(jsonPath("$[0].itens").doesNotExist());

        mockMvc.perform(get("/nfe/1").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chaveAcesso").value(CHAVE))
                .andExpect(jsonPath("$.itens", hasSize(3)));
    }

    @Test
    void consultaDeNfeInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/nfe/99").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("NF-e não encontrada."));
    }

    // Fornecedor e reimportação

    @Test
    void fornecedorNaoCadastradoRetorna422() throws Exception {
        when(fornecedorRepository.findByCnpj(CNPJ_EMITENTE)).thenReturn(Optional.empty());

        importar(admin, arquivo(bytes()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("O emitente da NF-e (CNPJ " + CNPJ_EMITENTE
                        + ") não está cadastrado como fornecedor. Cadastre o fornecedor e importe a NF-e novamente."));
        assertThat(banco).isEmpty();
    }

    @Test
    void reimportacaoDaMesmaNfeRetorna409() throws Exception {
        importar(admin, arquivo(bytes())).andExpect(status().isCreated());

        importar(admin, arquivo(bytes()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Esta NF-e (mesma chave de acesso) já foi importada."));
        assertThat(banco).hasSize(1);
    }

    // CT10 / CT11 — Arquivo inválido

    @Test
    void ct10_xmlMalformadoRetorna400() throws Exception {
        importar(admin, arquivo(bytes("<nfeProc><NFe>")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("O arquivo enviado não é um XML válido."));
        assertThat(banco).isEmpty();
    }

    @Test
    void ct10_xmlQueNaoEhNfeRetorna400() throws Exception {
        importar(admin, arquivo(bytes("<pedido><numero>1</numero></pedido>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O XML enviado não é uma NF-e."));
    }

    @Test
    void ct10_arquivoVazioRetorna400() throws Exception {
        importar(admin, arquivo(new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O arquivo enviado está vazio."));
    }

    @Test
    void ct10_extensaoXmlComConteudoDeOutroTipoRetorna400() throws Exception {
        importar(admin, new MockMultipartFile("arquivo", "nota.xml", "application/xml", bytes("%PDF-1.4")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O arquivo enviado não é um XML válido."));
    }

    @Test
    void ct10_tipoDeArquivoNaoAceitoRetorna400() throws Exception {
        importar(admin, new MockMultipartFile("arquivo", "nota.pdf", "application/pdf", bytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tipo de arquivo não aceito. Envie o XML da NF-e."));
        assertThat(banco).isEmpty();
    }

    @Test
    void ct10_semArquivoRetorna400() throws Exception {
        importar(admin, new MockMultipartFile("outroCampo", "nota.xml", "application/xml", bytes()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("O arquivo não foi enviado. Envie-o no campo \"arquivo\"."));
        assertThat(banco).isEmpty();
    }

    @Test
    void ct10_requisicaoQueNaoEhMultipartRetorna415() throws Exception {
        mockMvc.perform(post("/nfe").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_XML)
                        .content(bytes()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void ct11_campoObrigatorioAusenteRetorna400() throws Exception {
        importar(admin, arquivo(bytes(trocar("<vProd>201.00</vProd>", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("NF-e inválida: campo obrigatório ausente (det[nItem=1]/prod/vProd)."));
        assertThat(banco).isEmpty();
    }

    @Test
    void xmlComEntidadeExternaEhRejeitadoSemExporDetalhes() throws Exception {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE nfeProc [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe"><x>&xxe;</x></nfeProc>
                """;

        importar(admin, arquivo(bytes(xxe)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O arquivo enviado não é um XML válido."));
    }

    // CT03 / CT04 — Controle de acesso

    @Test
    void ct04_userNaoImportaNfe() throws Exception {
        importar(user, arquivo(bytes()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
        assertThat(banco).isEmpty();
    }

    @Test
    void ct04_userNaoConsultaNfe() throws Exception {
        mockMvc.perform(get("/nfe").header("Authorization", bearer(user))).andExpect(status().isForbidden());
        mockMvc.perform(get("/nfe/1").header("Authorization", bearer(user))).andExpect(status().isForbidden());
    }

    @Test
    void semAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(multipart("/nfe").file(arquivo(bytes())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/nfe")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/nfe/1")).andExpect(status().isUnauthorized());
        assertThat(banco).isEmpty();
    }

    private ResultActions importar(Usuario usuario, MockMultipartFile arquivo) throws Exception {
        return mockMvc.perform(multipart("/nfe").file(arquivo).header("Authorization", bearer(usuario)));
    }

    private static MockMultipartFile arquivo(byte[] conteudo) {
        return new MockMultipartFile("arquivo", "nfe-teste.xml", "application/xml", conteudo);
    }

    private void simularRepositoriosEmMemoria() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fornecedorRepository.findByCnpj(CNPJ_EMITENTE)).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findByGtin(anyString())).thenReturn(Optional.empty());

        when(nfeRepository.saveAndFlush(any(Nfe.class))).thenAnswer(inv -> gravar(inv.getArgument(0)));
        when(nfeRepository.existsByChaveAcesso(anyString())).thenAnswer(inv -> banco.stream()
                .anyMatch(n -> n.getChaveAcesso().equals(inv.getArgument(0))));
        when(nfeRepository.findDetalhadaById(anyLong())).thenAnswer(inv -> banco.stream()
                .filter(n -> n.getId().equals(inv.getArgument(0))).findFirst());
        when(nfeRepository.findAllByOrderByDataEmissaoDescIdDesc()).thenAnswer(inv -> banco.stream()
                .sorted(Comparator.comparing(Nfe::getDataEmissao).reversed())
                .toList());
    }

    /** Simula o banco: gera ids e o horário de importação. */
    private Nfe gravar(Nfe nfe) {
        ReflectionTestUtils.setField(nfe, "id", sequencia.incrementAndGet());
        ReflectionTestUtils.setField(nfe, "importadoEm", Instant.parse("2026-09-24T12:00:00Z"));
        long idItem = 100;
        for (NfeItem item : nfe.getItens()) {
            ReflectionTestUtils.setField(item, "id", ++idItem);
        }
        banco.add(nfe);
        return nfe;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario).token();
    }

    private static Usuario usuario(Long id, String nome, String email, Perfil perfil) {
        Usuario usuario = new Usuario(nome, email, "hash-nao-utilizado", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
