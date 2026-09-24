package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.config.SecurityErrorHandler;
import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.repository.ParametroCalculoRepository;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import br.com.squadcore.comparaprecos.service.ParametroCalculoService;
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

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parâmetros gerais do cálculo com a configuração de segurança real.
 * O repositório é simulado em memória com as mesmas chaves e valores da migration V6.
 */
@WebMvcTest(controllers = ParametroCalculoController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, TokenService.class, UsuarioDetailsService.class,
        ParametroCalculoService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=segredo-somente-para-testes-com-mais-de-32-caracteres",
        "app.jwt.expiracao=1h"
})
class ParametroCalculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private ParametroCalculoRepository parametroRepository;

    private final Map<ChaveParametro, ParametroCalculo> banco = new EnumMap<>(ChaveParametro.class);

    private Usuario admin;
    private Usuario user;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, Perfil.ADMIN);
        user = usuario(2L, Perfil.USER);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(user));

        banco.put(ChaveParametro.UF_DESTINO, new ParametroCalculo(ChaveParametro.UF_DESTINO, "GO"));
        banco.put(ChaveParametro.FONTE_UF_ORIGEM, new ParametroCalculo(ChaveParametro.FONTE_UF_ORIGEM, "EMITENTE_NFE"));
        banco.put(ChaveParametro.ARREDONDAMENTO_CREDITOS, new ParametroCalculo(ChaveParametro.ARREDONDAMENTO_CREDITOS, null));
        banco.put(ChaveParametro.CFOPS_PARTICIPANTES, new ParametroCalculo(ChaveParametro.CFOPS_PARTICIPANTES, null));
        when(parametroRepository.findAll()).thenAnswer(inv -> banco.values().stream().toList());
        when(parametroRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(banco.get(inv.getArgument(0))));
        when(parametroRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void userListaParametrosComSituacaoEValoresAceitos() throws Exception {
        mockMvc.perform(get("/parametros-calculo").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].chave").value("UF_DESTINO"))
                .andExpect(jsonPath("$[0].valor").value("GO"))
                .andExpect(jsonPath("$[0].definido").value(true))
                .andExpect(jsonPath("$[2].chave").value("ARREDONDAMENTO_CREDITOS"))
                .andExpect(jsonPath("$[2].definido").value(false))
                .andExpect(jsonPath("$[2].valoresAceitos[0]").value("POR_CREDITO"))
                .andExpect(jsonPath("$[2].valoresAceitos[1]").value("SOMENTE_TOTAL"));
    }

    @Test
    void buscaPorChaveSemDiferenciarMaiusculasE404ParaDesconhecida() throws Exception {
        mockMvc.perform(get("/parametros-calculo/uf_destino").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chave").value("UF_DESTINO"));
        mockMvc.perform(get("/parametros-calculo/NAO_EXISTE").header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Parâmetro de cálculo não encontrado."));
    }

    @Test
    void adminDefineParametroPendente() throws Exception {
        mockMvc.perform(put("/parametros-calculo/ARREDONDAMENTO_CREDITOS").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": " por_credito "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("POR_CREDITO"))
                .andExpect(jsonPath("$.definido").value(true));
        assertThat(banco.get(ChaveParametro.ARREDONDAMENTO_CREDITOS).getValor()).isEqualTo("POR_CREDITO");
    }

    @Test
    void adminDefineListaDeCfopsNormalizada() throws Exception {
        mockMvc.perform(put("/parametros-calculo/CFOPS_PARTICIPANTES").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": "2222, 1111"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value("1111,2222"));
    }

    @Test
    void adminVoltaParametroParaNaoDefinido() throws Exception {
        mockMvc.perform(put("/parametros-calculo/FONTE_UF_ORIGEM").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(nullValue()))
                .andExpect(jsonPath("$.definido").value(false));
    }

    @Test
    void rejeitaValorForaDosAceitos() throws Exception {
        mockMvc.perform(put("/parametros-calculo/UF_DESTINO").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": "XX"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").exists());
        assertThat(banco.get(ChaveParametro.UF_DESTINO).getValor()).isEqualTo("GO");
        verify(parametroRepository, never()).saveAndFlush(any());
    }

    @Test
    void alterarChaveDesconhecidaRetorna404() throws Exception {
        mockMvc.perform(put("/parametros-calculo/NAO_EXISTE").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": "X"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void ct04_userNaoAlteraParametro() throws Exception {
        mockMvc.perform(put("/parametros-calculo/UF_DESTINO").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valor": "RJ"}
                                """))
                .andExpect(status().isForbidden());
        assertThat(banco.get(ChaveParametro.UF_DESTINO).getValor()).isEqualTo("GO");
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/parametros-calculo")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/parametros-calculo/UF_DESTINO").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\": \"RJ\"}"))
                .andExpect(status().isUnauthorized());
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
