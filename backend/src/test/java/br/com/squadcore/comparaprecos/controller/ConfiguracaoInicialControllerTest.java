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
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuração inicial (primeiro ADMIN) com a configuração de segurança real e o repositório
 * de usuários simulado. A concorrência contra o PostgreSQL real está em
 * ConfiguracaoInicialConcorrenciaTest. Nomes, e-mails e senhas são dados de teste.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, AuthService.class,
        UsuarioService.class, UsuarioDetailsService.class, ConfiguracaoInicialService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class ConfiguracaoInicialControllerTest {

    private static final String URL = "/auth/configuracao-inicial";
    private static final String SENHA_TESTE = "senha-de-teste-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario novo = inv.getArgument(0);
            ReflectionTestUtils.setField(novo, "id", 1L);
            return novo;
        });
    }

    @Test
    void semUsuariosInformaNaoConfigurado() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(false));
    }

    @Test
    void comUsuarioInformaConfiguradoSemExporDados() throws Exception {
        when(usuarioRepository.count()).thenReturn(1L);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurado").value(true))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nome").doesNotExist());
    }

    @Test
    void criaPrimeiroAdminComSenhaEmBCryptSemPrecisarDeToken() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("  Admin Teste ", "Admin@Teste.Local", SENHA_TESTE, SENHA_TESTE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Admin Teste"))
                .andExpect(jsonPath("$.email").value("admin@teste.local"))
                .andExpect(jsonPath("$.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(salvo.getValue().getSenhaHash()).isNotEqualTo(SENHA_TESTE).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches(SENHA_TESTE, salvo.getValue().getSenhaHash())).isTrue();
    }

    @Test
    void verificaSeHaUsuariosSomenteDepoisDeBloquearATabela() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Admin Teste", "admin@teste.local", SENHA_TESTE, SENHA_TESTE)))
                .andExpect(status().isCreated());

        InOrder ordem = inOrder(usuarioRepository);
        ordem.verify(usuarioRepository).bloquearTabela();
        ordem.verify(usuarioRepository).count();
        ordem.verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void perfilEnviadoNoCorpoEIgnorado() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Admin Teste", "email": "admin@teste.local", "senha": "%s",
                                 "confirmacaoSenha": "%s", "perfil": "USER"}
                                """.formatted(SENHA_TESTE, SENHA_TESTE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void sistemaJaConfiguradoRejeitaCriacao() throws Exception {
        when(usuarioRepository.count()).thenReturn(1L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outro Admin", "outro@teste.local", SENHA_TESTE, SENHA_TESTE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O sistema já foi configurado. Entre com um usuário existente."));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void emailInvalidoERejeitado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Admin Teste", "nao-e-email", SENHA_TESTE, SENHA_TESTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").value("O e-mail informado é inválido."));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void nomeVazioERejeitado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("   ", "admin@teste.local", SENHA_TESTE, SENHA_TESTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").value("O nome é obrigatório."));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaForaDoTamanhoERejeitada() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Admin Teste", "admin@teste.local", "curta", "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").value("A senha deve ter entre 8 e 72 caracteres."));

        String longa = "a".repeat(73);
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Admin Teste", "admin@teste.local", longa, longa)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").exists());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void camposAusentesSaoRejeitados() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").exists())
                .andExpect(jsonPath("$.erros.confirmacaoSenha").value("A confirmação da senha é obrigatória."));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void confirmacaoDiferenteERejeitada() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Admin Teste", "admin@teste.local", SENHA_TESTE, "outra-senha-123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.confirmacaoSenha").value("A confirmação da senha não confere."));
        verify(usuarioRepository, never()).save(any());
    }

    private static String corpo(String nome, String email, String senha, String confirmacao) {
        return """
                {"nome": "%s", "email": "%s", "senha": "%s", "confirmacaoSenha": "%s"}
                """.formatted(nome, email, senha, confirmacao);
    }
}
