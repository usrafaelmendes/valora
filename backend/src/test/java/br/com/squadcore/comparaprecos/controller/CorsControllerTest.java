package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.AuthService;
import br.com.squadcore.comparaprecos.service.ConfiguracaoInicialService;
import br.com.squadcore.comparaprecos.service.TokenService;
import br.com.squadcore.comparaprecos.service.UsuarioDetailsService;
import br.com.squadcore.comparaprecos.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS para o frontend desktop (Tauri), que chama o backend local diretamente: somente as origens
 * configuradas são aceitas. As origens abaixo são as do padrão de application.yml.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, AuthService.class,
        UsuarioService.class, UsuarioDetailsService.class, ConfiguracaoInicialService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h",
        "app.cors.origens-permitidas=tauri://localhost,http://tauri.localhost,http://localhost:5173"
})
class CorsControllerTest {

    private static final String URL = "/auth/configuracao-inicial";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void preflightDaOrigemDesktopEhAceitoSemAutenticacao() throws Exception {
        mockMvc.perform(options("/usuarios")
                        .header(HttpHeaders.ORIGIN, "tauri://localhost")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "tauri://localhost"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void requisicaoDaOrigemDesktopWindowsExpoeContentDisposition() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(get(URL).header(HttpHeaders.ORIGIN, "http://tauri.localhost"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://tauri.localhost"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Content-Disposition")));
    }

    @Test
    void origemDoViteContinuaAceita() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        mockMvc.perform(get(URL).header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void origemNaoConfiguradaEhRecusada() throws Exception {
        mockMvc.perform(options(URL)
                        .header(HttpHeaders.ORIGIN, "http://exemplo.invalido")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        mockMvc.perform(get(URL).header(HttpHeaders.ORIGIN, "http://exemplo.invalido"))
                .andExpect(status().isForbidden());
    }
}
