package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.AuthService;
import br.com.squadcore.comparaprecos.service.ConfiguracaoInicialService;
import br.com.squadcore.comparaprecos.service.TokenService;
import br.com.squadcore.comparaprecos.service.UsuarioDetailsService;
import br.com.squadcore.comparaprecos.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação e autorização (CT01–CT04) com a configuração de segurança real
 * e o repositório de usuários simulado (sem banco de dados).
 * Os usuários e senhas abaixo são dados de teste.
 */
@WebMvcTest(controllers = {AuthController.class, UsuarioController.class})
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, AuthService.class,
        UsuarioService.class, UsuarioDetailsService.class, ConfiguracaoInicialService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class AutenticacaoControllerTest {

    private static final String SENHA_TESTE = "senha-de-teste-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private Usuario admin;
    private Usuario user;

    @BeforeEach
    void setUp() {
        String hash = new BCryptPasswordEncoder().encode(SENHA_TESTE);
        admin = usuario(1L, "Admin Teste", "admin@teste.local", hash, Perfil.ADMIN);
        user = usuario(2L, "User Teste", "user@teste.local", hash, Perfil.USER);

        when(usuarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase(eq("admin@teste.local"))).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByEmailIgnoreCase(eq("user@teste.local"))).thenReturn(Optional.of(user));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));
    }

    // CT01 — Login válido

    @Test
    void ct01_loginValidoRetornaTokenEPerfil() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@teste.local", SENHA_TESTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.usuario.email").value("admin@teste.local"))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
    }

    @Test
    void ct01_loginIgnoraMaiusculasEEspacosNoEmail() throws Exception {
        when(usuarioRepository.findByEmailIgnoreCase(eq("USER@teste.local"))).thenReturn(Optional.of(user));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("  USER@teste.local ", SENHA_TESTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.perfil").value("USER"));
    }

    @Test
    void ct01_tokenDaAcessoAoUsuarioAutenticado() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@teste.local"))
                .andExpect(jsonPath("$.perfil").value("USER"));
    }

    // CT02 — Login inválido

    @Test
    void ct02_senhaIncorretaRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@teste.local", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void ct02_emailInexistenteRetornaMesmaMensagem() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("naoexiste@teste.local", SENHA_TESTE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void ct02_usuarioInativoNaoConsegueLogar() throws Exception {
        user.setAtivo(false);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("user@teste.local", SENHA_TESTE)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void ct02_tokenDeUsuarioDesativadoNaoAcessaMe() throws Exception {
        String token = bearer(user);
        user.setAtivo(false);

        mockMvc.perform(get("/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSemCamposRetorna400ComErrosPorCampo() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").value("O e-mail é obrigatório."))
                .andExpect(jsonPath("$.erros.senha").value("A senha é obrigatória."));
    }

    // CT03 — Acesso administrativo

    @Test
    void ct03_adminListaUsuarios() throws Exception {
        when(usuarioRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(admin, user));

        mockMvc.perform(get("/usuarios").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
    }

    @Test
    void ct03_adminCriaUsuarioComSenhaEmBCrypt() throws Exception {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario novo = inv.getArgument(0);
            ReflectionTestUtils.setField(novo, "id", 3L);
            return novo;
        });

        mockMvc.perform(post("/usuarios").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Novo Teste", "email": "Novo@Teste.Local",
                                 "senha": "outra-senha-123", "perfil": "USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("novo@teste.local"))
                .andExpect(jsonPath("$.perfil").value("USER"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getSenhaHash()).isNotEqualTo("outra-senha-123").startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("outra-senha-123", salvo.getValue().getSenhaHash())).isTrue();
    }

    @Test
    void adminNaoCriaUsuarioComEmailDuplicado() throws Exception {
        when(usuarioRepository.existsByEmailIgnoreCase("user@teste.local")).thenReturn(true);

        mockMvc.perform(post("/usuarios").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Dup", "email": "user@teste.local",
                                 "senha": "outra-senha-123", "perfil": "USER"}
                                """))
                .andExpect(status().isConflict());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void adminNaoCriaUsuarioComDadosInvalidos() throws Exception {
        mockMvc.perform(post("/usuarios").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "", "email": "nao-e-email", "senha": "curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").exists())
                .andExpect(jsonPath("$.erros.perfil").exists());
        verify(usuarioRepository, never()).save(any());
    }

    // CT04 — Bloqueio de acesso administrativo

    @Test
    void ct04_userNaoListaUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso."));
    }

    @Test
    void ct04_userNaoCriaUsuario() throws Exception {
        mockMvc.perform(post("/usuarios").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "X", "email": "x@teste.local", "senha": "senha-123456", "perfil": "ADMIN"}
                                """))
                .andExpect(status().isForbidden());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer token.invalido.qualquer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOutroSegredoRetorna401() throws Exception {
        // Token HS256 válido, porém assinado com um segredo diferente do configurado.
        String tokenForjado = "eyJhbGciOiJIUzI1NiJ9."
                + "eyJzdWIiOiIxIiwicGVyZmlsIjoiQURNSU4iLCJleHAiOjQxMDI0NDQ4MDB9."
                + "c2VtLWFzc2luYXR1cmEtdmFsaWRhLXBhcmEtZXN0ZS1zZWdyZWRv";
        mockMvc.perform(get("/usuarios").header("Authorization", "Bearer " + tokenForjado))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario).token();
    }

    private static String login(String email, String senha) {
        return """
                {"email": "%s", "senha": "%s"}
                """.formatted(email, senha);
    }

    private static Usuario usuario(Long id, String nome, String email, String hash, Perfil perfil) {
        Usuario usuario = new Usuario(nome, email, hash, perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
