package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.Fornecedor;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.TipoFornecedor;
import br.com.squadcore.comparaprecos.entity.Uf;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.FornecedorRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.FornecedorService;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
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
 * Fornecedores (CT05, CT06, CT22, CT03/CT04 aplicados ao recurso) com a configuração de segurança real.
 * O repositório é simulado em memória (sem banco); a integração com o PostgreSQL é validada à parte.
 * Todos os nomes e CNPJs abaixo são fictícios (dados de teste).
 */
@WebMvcTest(controllers = FornecedorController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        FornecedorService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class FornecedorControllerTest {

    private static final String CNPJ_1 = "11222333000181";
    private static final String CNPJ_2 = "12345678000195";
    private static final String CNPJ_3 = "98765432000198";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private FornecedorRepository fornecedorRepository;

    private final List<Fornecedor> banco = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong();

    private Usuario admin;
    private Usuario user;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, "Admin Teste", "admin@teste.local", Perfil.ADMIN);
        user = usuario(2L, "User Teste", "user@teste.local", Perfil.USER);
        simularRepositorioEmMemoria();
    }

    // CT05 — Cadastro de fornecedor

    @Test
    void ct05_adminCadastraFornecedorComCnpjComMascara() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  Fornecedor Teste Ltda  ", "11.222.333/0001-81", "ES", "FABRICANTE",
                                " 28/56/84 dias ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.razaoSocial").value("Fornecedor Teste Ltda"))
                .andExpect(jsonPath("$.cnpj").value(CNPJ_1))
                .andExpect(jsonPath("$.uf").value("ES"))
                .andExpect(jsonPath("$.tipo").value("FABRICANTE"))
                .andExpect(jsonPath("$.prazoPagamentoBase").value("28/56/84 dias"))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(banco).hasSize(1);
        assertThat(banco.getFirst().getCnpj()).isEqualTo(CNPJ_1);
    }

    @Test
    void ct05_cadastraCnpjAlfanumericoNormalizado() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor Alfa", "12.abc.345/01de-35", "RJ", "ATACADISTA", "30 dias")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("12ABC34501DE35"))
                .andExpect(jsonPath("$.tipo").value("ATACADISTA"));
    }

    @Test
    void ct05_fornecedorCadastradoPodeSerConsultadoPosteriormente() throws Exception {
        Fornecedor salvo = existente("Fornecedor Teste", CNPJ_1, Uf.MG, TipoFornecedor.ATACADISTA, "60 dias");

        mockMvc.perform(get("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salvo.getId()))
                .andExpect(jsonPath("$.cnpj").value(CNPJ_1))
                .andExpect(jsonPath("$.uf").value("MG"))
                .andExpect(jsonPath("$.tipo").value("ATACADISTA"))
                .andExpect(jsonPath("$.prazoPagamentoBase").value("60 dias"));
    }

    // CT22 — prazo de pagamento ausente não é erro

    @Test
    void ct22_cadastraSemPrazoDePagamento() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razaoSocial": "Sem Prazo", "cnpj": "%s", "uf": "RJ", "tipo": "FABRICANTE"}
                                """.formatted(CNPJ_1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prazoPagamentoBase").value(nullValue()));
    }

    @Test
    void ct22_prazoEmBrancoEhTratadoComoAusente() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Prazo Em Branco", CNPJ_1, "RJ", "FABRICANTE", "   ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prazoPagamentoBase").value(nullValue()));
    }

    // CT06 — Validação de fornecedor

    @Test
    void ct06_rejeitaCamposObrigatoriosAusentes() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.razaoSocial").value("A razão social é obrigatória."))
                .andExpect(jsonPath("$.erros.cnpj").value("O CNPJ é obrigatório."))
                .andExpect(jsonPath("$.erros.uf").exists())
                .andExpect(jsonPath("$.erros.tipo").exists());
        verify(fornecedorRepository, never()).saveAndFlush(any());
    }

    @Test
    void ct06_rejeitaCnpjComDigitoVerificadorInvalido() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", "11.222.333/0001-82", "RJ", "FABRICANTE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cnpj").value("O CNPJ informado é inválido."));
        verify(fornecedorRepository, never()).saveAndFlush(any());
    }

    @Test
    void ct06_rejeitaCnpjComFormatoInvalido() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", "00000000000000", "RJ", "FABRICANTE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cnpj").exists());
    }

    @Test
    void ct06_rejeitaUfInexistente() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", CNPJ_1, "XX", "FABRICANTE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.uf").value(startsWith("Valor inválido. Valores aceitos: AC,")));
        verify(fornecedorRepository, never()).saveAndFlush(any());
    }

    @Test
    void ct06_rejeitaTipoNaoPrevisto() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", CNPJ_1, "RJ", "DISTRIBUIDOR", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.tipo").value("Valor inválido. Valores aceitos: FABRICANTE, ATACADISTA."));
        verify(fornecedorRepository, never()).saveAndFlush(any());
    }

    @Test
    void ct06_rejeitaCamposAcimaDoTamanhoMaximo() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("R".repeat(151), CNPJ_1, "RJ", "FABRICANTE", "P".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.razaoSocial").exists())
                .andExpect(jsonPath("$.erros.prazoPagamentoBase").exists());
    }

    @Test
    void ct06_rejeitaCorpoMalFormatado() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // CNPJ duplicado

    @Test
    void rejeitaCnpjDuplicadoMesmoComMascaraDiferente() throws Exception {
        existente("Original", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);

        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Duplicado", "11.222.333/0001-81", "RJ", "FABRICANTE", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um fornecedor cadastrado com este CNPJ."));
        assertThat(banco).hasSize(1);
    }

    @Test
    void rejeitaCnpjDeFornecedorDesativado() throws Exception {
        Fornecedor desativado = existente("Antigo", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);
        desativado.setAtivo(false);

        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Novo", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um fornecedor desativado cadastrado com este CNPJ."));
    }

    @Test
    void cadastroSimultaneoComMesmoCnpjViolaIndiceUnicoERetorna409() throws Exception {
        when(fornecedorRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_fornecedor_cnpj\""));

        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Concorrente", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isConflict());
    }

    // Listagem

    @Test
    void listaSomenteAtivosOrdenadosPorRazaoSocial() throws Exception {
        existente("Zeta Fornecedor Teste", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);
        existente("Alfa Distribuidora", CNPJ_2, Uf.PR, TipoFornecedor.ATACADISTA, "30 dias");
        existente("Inativo SA", CNPJ_3, Uf.RS, TipoFornecedor.FABRICANTE, null).setAtivo(false);

        mockMvc.perform(get("/fornecedores").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].razaoSocial").value("Alfa Distribuidora"))
                .andExpect(jsonPath("$[1].razaoSocial").value("Zeta Fornecedor Teste"));
    }

    // Atualização

    @Test
    void adminAtualizaFornecedor() throws Exception {
        Fornecedor salvo = existente("Nome Antigo", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, "30 dias");

        mockMvc.perform(put("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nome Novo", CNPJ_1, "AM", "ATACADISTA", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Nome Novo"))
                .andExpect(jsonPath("$.cnpj").value(CNPJ_1))
                .andExpect(jsonPath("$.uf").value("AM"))
                .andExpect(jsonPath("$.tipo").value("ATACADISTA"))
                .andExpect(jsonPath("$.prazoPagamentoBase").value(nullValue()));

        assertThat(salvo.getRazaoSocial()).isEqualTo("Nome Novo");
        assertThat(salvo.getUf()).isEqualTo(Uf.AM);
    }

    @Test
    void atualizacaoNaoPodeUsarCnpjDeOutroFornecedor() throws Exception {
        existente("Primeiro", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);
        Fornecedor segundo = existente("Segundo", CNPJ_2, Uf.RJ, TipoFornecedor.FABRICANTE, null);

        mockMvc.perform(put("/fornecedores/" + segundo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Segundo", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isConflict());
        assertThat(segundo.getCnpj()).isEqualTo(CNPJ_2);
    }

    @Test
    void atualizacaoValidaOsDados() throws Exception {
        Fornecedor salvo = existente("Fornecedor", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);

        mockMvc.perform(put("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("", "123", "RJ", "FABRICANTE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.razaoSocial").exists())
                .andExpect(jsonPath("$.erros.cnpj").exists());
        assertThat(salvo.getRazaoSocial()).isEqualTo("Fornecedor");
    }

    @Test
    void atualizarFornecedorInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/fornecedores/999").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Fornecedor não encontrado."));
    }

    // Desativação

    @Test
    void adminDesativaFornecedorSemApagarORegistro() throws Exception {
        Fornecedor salvo = existente("Fornecedor", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);

        mockMvc.perform(delete("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(banco).containsExactly(salvo);
        assertThat(salvo.isAtivo()).isFalse();
        verify(fornecedorRepository, never()).delete(any());
        verify(fornecedorRepository, never()).deleteById(anyLong());
    }

    @Test
    void fornecedorDesativadoNaoApareceNasConsultasNemPodeSerAlterado() throws Exception {
        Fornecedor salvo = existente("Fornecedor", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);
        mockMvc.perform(delete("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/fornecedores").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(put("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Outro Nome", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/fornecedores/" + salvo.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        assertThat(salvo.getRazaoSocial()).isEqualTo("Fornecedor");
    }

    @Test
    void consultarFornecedorInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/fornecedores/999").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get("/fornecedores/abc").header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest());
    }

    // Acesso: USER consulta; operações administrativas exigem ADMIN (CT03, CT04)

    @Test
    void userConsultaEListaFornecedores() throws Exception {
        Fornecedor salvo = existente("Fornecedor", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, "90 dias");

        mockMvc.perform(get("/fornecedores").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cnpj").value(CNPJ_1));
        mockMvc.perform(get("/fornecedores/" + salvo.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Fornecedor"));
    }

    @Test
    void ct04_userNaoCriaFornecedor() throws Exception {
        mockMvc.perform(post("/fornecedores").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
        assertThat(banco).isEmpty();
    }

    @Test
    void ct04_userNaoAtualizaFornecedor() throws Exception {
        Fornecedor salvo = existente("Fornecedor", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);

        mockMvc.perform(put("/fornecedores/" + salvo.getId()).header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Alterado", CNPJ_1, "RJ", "ATACADISTA", null)))
                .andExpect(status().isForbidden());
        assertThat(salvo.getRazaoSocial()).isEqualTo("Fornecedor");
    }

    @Test
    void ct04_userNaoDesativaFornecedor() throws Exception {
        Fornecedor salvo = existente("Fornecedor", CNPJ_1, Uf.RJ, TipoFornecedor.FABRICANTE, null);

        mockMvc.perform(delete("/fornecedores/" + salvo.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/fornecedores")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/fornecedores/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/fornecedores").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Fornecedor", CNPJ_1, "RJ", "FABRICANTE", null)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/fornecedores/1")).andExpect(status().isUnauthorized());
        assertThat(banco).isEmpty();
    }

    /** Repositório em memória com o comportamento dos métodos usados pelo FornecedorService. */
    private void simularRepositorioEmMemoria() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));

        when(fornecedorRepository.saveAndFlush(any(Fornecedor.class))).thenAnswer(inv -> gravar(inv.getArgument(0)));
        when(fornecedorRepository.save(any(Fornecedor.class))).thenAnswer(inv -> gravar(inv.getArgument(0)));
        when(fornecedorRepository.findByCnpj(anyString())).thenAnswer(inv -> banco.stream()
                .filter(f -> f.getCnpj().equals(inv.getArgument(0))).findFirst());
        when(fornecedorRepository.findByIdAndAtivoTrue(anyLong())).thenAnswer(inv -> banco.stream()
                .filter(f -> f.getId().equals(inv.getArgument(0)) && f.isAtivo()).findFirst());
        when(fornecedorRepository.findByAtivoTrueOrderByRazaoSocialAscIdAsc()).thenAnswer(inv -> banco.stream()
                .filter(Fornecedor::isAtivo)
                .sorted(Comparator.comparing(Fornecedor::getRazaoSocial).thenComparing(Fornecedor::getId))
                .toList());
    }

    private Fornecedor gravar(Fornecedor fornecedor) {
        if (fornecedor.getId() == null) {
            ReflectionTestUtils.setField(fornecedor, "id", sequencia.incrementAndGet());
            banco.add(fornecedor);
        }
        return fornecedor;
    }

    private Fornecedor existente(String razaoSocial, String cnpj, Uf uf, TipoFornecedor tipo, String prazo) {
        return gravar(new Fornecedor(razaoSocial, cnpj, uf, tipo, prazo));
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario).token();
    }

    private static String json(String razaoSocial, String cnpj, String uf, String tipo, String prazo) {
        return """
                {"razaoSocial": "%s", "cnpj": "%s", "uf": "%s", "tipo": "%s", "prazoPagamentoBase": %s}
                """.formatted(razaoSocial, cnpj, uf, tipo, prazo == null ? "null" : "\"" + prazo + "\"");
    }

    private static Usuario usuario(Long id, String nome, String email, Perfil perfil) {
        Usuario usuario = new Usuario(nome, email, "hash-nao-utilizado", perfil);
        ReflectionTestUtils.setField(usuario, "id", Objects.requireNonNull(id));
        return usuario;
    }
}
