package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Produto;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.ProdutoRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.ProdutoService;
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
 * Produtos (CT07, CT08, CT03/CT04 aplicados ao recurso) com a configuração de segurança real.
 * O repositório é simulado em memória (sem banco); a integração com o PostgreSQL é validada à parte.
 * Nomes e GTINs abaixo são dados de teste.
 */
@WebMvcTest(controllers = ProdutoController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        ProdutoService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class ProdutoControllerTest {

    private static final String GTIN_1 = "4006381333931";
    private static final String GTIN_2 = "7891000315507";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ProdutoRepository produtoRepository;

    private final List<Produto> banco = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong();

    private Usuario admin;
    private Usuario user;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, "Admin Teste", "admin@teste.local", Perfil.ADMIN);
        user = usuario(2L, "User Teste", "user@teste.local", Perfil.USER);
        simularRepositorioEmMemoria();
    }

    // CT07 — Cadastro de produto

    @Test
    void ct07_adminCadastraProduto() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  Produto Teste X  ", " Descrição de teste ", " " + GTIN_1 + " ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Produto Teste X"))
                .andExpect(jsonPath("$.descricao").value("Descrição de teste"))
                .andExpect(jsonPath("$.gtin").value(GTIN_1))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(banco).hasSize(1);
    }

    @Test
    void ct07_cadastraSemDescricaoESemGtin() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Produto Teste C"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(nullValue()))
                .andExpect(jsonPath("$.gtin").value(nullValue()));
    }

    @Test
    void ct07_textosOpcionaisEmBrancoSaoTratadosComoAusentes() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto Teste B", "   ", "  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value(nullValue()))
                .andExpect(jsonPath("$.gtin").value(nullValue()));
    }

    @Test
    void ct07_produtoCadastradoPodeSerConsultadoPosteriormente() throws Exception {
        Produto salvo = existente("Placa-mãe Teste", "ATX", GTIN_1);

        mockMvc.perform(get("/produtos/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salvo.getId()))
                .andExpect(jsonPath("$.nome").value("Placa-mãe Teste"))
                .andExpect(jsonPath("$.descricao").value("ATX"))
                .andExpect(jsonPath("$.gtin").value(GTIN_1));
    }

    // CT08 — Validação de produto

    @Test
    void ct08_rejeitaNomeAusente() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").value("O nome é obrigatório."));
        verify(produtoRepository, never()).saveAndFlush(any());
    }

    @Test
    void ct08_rejeitaNomeEmBranco() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("   ", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());
    }

    @Test
    void ct08_rejeitaGtinComDigitoVerificadorInvalido() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto Teste A", null, "4006381333932")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.gtin")
                        .value("O GTIN/EAN informado é inválido (use 8, 12, 13 ou 14 dígitos)."));
        verify(produtoRepository, never()).saveAndFlush(any());
    }

    @Test
    void ct08_rejeitaGtinComFormatoInvalido() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto Teste A", null, "SEM GTIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.gtin").exists());
    }

    @Test
    void ct08_rejeitaCamposAcimaDoTamanhoMaximo() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("N".repeat(151), "D".repeat(501), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.descricao").exists());
    }

    @Test
    void ct08_rejeitaTipoIncompativelNoCampo() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": {"texto": "objeto no lugar de texto"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists());
    }

    @Test
    void ct08_rejeitaCorpoMalFormatado() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // Conflitos

    @Test
    void rejeitaNomeDuplicadoSemDiferenciarMaiusculas() throws Exception {
        existente("Produto Teste Grande", null, null);

        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  produto teste grande ", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um produto cadastrado com este nome."));
        assertThat(banco).hasSize(1);
    }

    @Test
    void rejeitaGtinDuplicado() throws Exception {
        existente("Produto A", null, GTIN_1);

        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto B", null, GTIN_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um produto cadastrado com este GTIN/EAN."));
    }

    @Test
    void rejeitaNomeDeProdutoDesativado() throws Exception {
        existente("Gabinete Teste", null, null).setAtivo(false);

        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Gabinete Teste", null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um produto desativado cadastrado com este nome."));
    }

    @Test
    void produtosSemGtinNaoConflitamEntreSi() throws Exception {
        existente("HD Teste 1", null, null);

        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("HD Teste 2", null, null)))
                .andExpect(status().isCreated());
        verify(produtoRepository, never()).findByGtin(anyString());
    }

    @Test
    void cadastroSimultaneoQueViolaIndiceUnicoRetorna409() throws Exception {
        when(produtoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_produto_gtin\""));

        mockMvc.perform(post("/produtos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Concorrente", null, GTIN_1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um produto cadastrado com este GTIN/EAN."));
    }

    // Listagem

    @Test
    void listaSomenteAtivosOrdenadosPorNome() throws Exception {
        existente("Produto Teste B", null, null);
        existente("Produto Teste A", null, GTIN_1);
        existente("Antigo Teste", null, GTIN_2).setAtivo(false);

        mockMvc.perform(get("/produtos").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Produto Teste A"))
                .andExpect(jsonPath("$[1].nome").value("Produto Teste B"));
    }

    // Atualização

    @Test
    void adminAtualizaProduto() throws Exception {
        Produto salvo = existente("Nome Antigo", "Descrição antiga", GTIN_1);

        mockMvc.perform(put("/produtos/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nome Novo", null, GTIN_2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.descricao").value(nullValue()))
                .andExpect(jsonPath("$.gtin").value(GTIN_2));

        assertThat(salvo.getNome()).isEqualTo("Nome Novo");
    }

    @Test
    void atualizacaoPermiteMudarSoMaiusculasDoProprioNome() throws Exception {
        Produto salvo = existente("produto teste d", null, GTIN_1);

        mockMvc.perform(put("/produtos/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto Teste D", null, GTIN_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Teste D"));
    }

    @Test
    void atualizacaoNaoPodeUsarNomeOuGtinDeOutroProduto() throws Exception {
        existente("Primeiro", null, GTIN_1);
        Produto segundo = existente("Segundo", null, GTIN_2);

        mockMvc.perform(put("/produtos/" + segundo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("PRIMEIRO", null, GTIN_2)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/produtos/" + segundo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Segundo", null, GTIN_1)))
                .andExpect(status().isConflict());
        assertThat(segundo.getNome()).isEqualTo("Segundo");
        assertThat(segundo.getGtin()).isEqualTo(GTIN_2);
    }

    @Test
    void atualizacaoValidaOsDados() throws Exception {
        Produto salvo = existente("Produto", null, null);

        mockMvc.perform(put("/produtos/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("", null, "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.gtin").exists());
        assertThat(salvo.getNome()).isEqualTo("Produto");
    }

    @Test
    void atualizarProdutoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/produtos/999").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto", null, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produto não encontrado."));
    }

    // Desativação

    @Test
    void adminDesativaProdutoSemApagarORegistro() throws Exception {
        Produto salvo = existente("Produto", null, null);

        mockMvc.perform(delete("/produtos/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(banco).containsExactly(salvo);
        assertThat(salvo.isAtivo()).isFalse();
        verify(produtoRepository, never()).delete(any());
        verify(produtoRepository, never()).deleteById(anyLong());
    }

    @Test
    void produtoDesativadoNaoApareceNasConsultasNemPodeSerAlterado() throws Exception {
        Produto salvo = existente("Produto", null, null);
        mockMvc.perform(delete("/produtos/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/produtos/" + salvo.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/produtos").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(put("/produtos/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Outro Nome", null, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/produtos/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        assertThat(salvo.getNome()).isEqualTo("Produto");
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/produtos/abc").header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
    }

    // Acesso: USER consulta; operações administrativas exigem ADMIN (CT03, CT04)

    @Test
    void userConsultaEListaProdutos() throws Exception {
        Produto salvo = existente("Produto", null, GTIN_1);

        mockMvc.perform(get("/produtos").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/produtos/" + salvo.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtin").value(GTIN_1));
    }

    @Test
    void ct04_userNaoCriaProduto() throws Exception {
        mockMvc.perform(post("/produtos").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto", null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
        assertThat(banco).isEmpty();
    }

    @Test
    void ct04_userNaoAtualizaProduto() throws Exception {
        Produto salvo = existente("Produto", null, null);

        mockMvc.perform(put("/produtos/" + salvo.getId()).header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Alterado", null, null)))
                .andExpect(status().isForbidden());
        assertThat(salvo.getNome()).isEqualTo("Produto");
    }

    @Test
    void ct04_userNaoDesativaProduto() throws Exception {
        Produto salvo = existente("Produto", null, null);

        mockMvc.perform(delete("/produtos/" + salvo.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/produtos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/produtos/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto", null, null)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/produtos/1").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Produto", null, null)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/produtos/1")).andExpect(status().isUnauthorized());
        assertThat(banco).isEmpty();
    }

    /** Repositório em memória com o comportamento dos métodos usados pelo ProdutoService. */
    private void simularRepositorioEmMemoria() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));

        when(produtoRepository.saveAndFlush(any(Produto.class))).thenAnswer(inv -> gravar(inv.getArgument(0)));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> gravar(inv.getArgument(0)));
        when(produtoRepository.findByNomeIgnoreCase(anyString())).thenAnswer(inv -> banco.stream()
                .filter(p -> p.getNome().equalsIgnoreCase(inv.getArgument(0))).findFirst());
        when(produtoRepository.findByGtin(anyString())).thenAnswer(inv -> banco.stream()
                .filter(p -> inv.getArgument(0).equals(p.getGtin())).findFirst());
        when(produtoRepository.findByIdAndAtivoTrue(anyLong())).thenAnswer(inv -> banco.stream()
                .filter(p -> p.getId().equals(inv.getArgument(0)) && p.isAtivo()).findFirst());
        when(produtoRepository.findByAtivoTrueOrderByNomeAscIdAsc()).thenAnswer(inv -> banco.stream()
                .filter(Produto::isAtivo)
                .sorted(Comparator.comparing(Produto::getNome).thenComparing(Produto::getId))
                .toList());
    }

    private Produto gravar(Produto produto) {
        if (produto.getId() == null) {
            ReflectionTestUtils.setField(produto, "id", sequencia.incrementAndGet());
            banco.add(produto);
        }
        return produto;
    }

    private Produto existente(String nome, String descricao, String gtin) {
        return gravar(new Produto(nome, descricao, gtin));
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario).token();
    }

    private static String json(String nome, String descricao, String gtin) {
        return """
                {"nome": %s, "descricao": %s, "gtin": %s}
                """.formatted(texto(nome), texto(descricao), texto(gtin));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static Usuario usuario(Long id, String nome, String email, Perfil perfil) {
        Usuario usuario = new Usuario(nome, email, "hash-nao-utilizado", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
