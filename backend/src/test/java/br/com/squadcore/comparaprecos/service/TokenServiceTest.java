package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.config.JwtProperties;
import br.com.squadcore.comparaprecos.config.SecurityConfig;
import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SEGREDO_TESTE = "segredo-somente-para-testes-com-mais-de-32-caracteres";

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtProperties properties = new JwtProperties(SEGREDO_TESTE, Duration.ofHours(8));
    private final JwtDecoder decoder = securityConfig.jwtDecoder(properties);

    @Test
    void tokenContemIdPerfilEExpiracaoConfigurada() {
        TokenService tokenService = new TokenService(securityConfig.jwtEncoder(properties), properties);

        TokenService.TokenGerado gerado = tokenService.gerar(usuario(Perfil.ADMIN));
        Jwt jwt = decoder.decode(gerado.token());

        assertThat(jwt.getSubject()).isEqualTo("10");
        assertThat(jwt.getClaimAsString(TokenService.CLAIM_PERFIL)).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString(TokenService.CLAIM_EMAIL)).isEqualTo("teste@teste.local");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
        assertThat(gerado.expiraEm()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void tokenExpiradoERejeitado() {
        Clock ontem = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        TokenService tokenService = new TokenService(securityConfig.jwtEncoder(properties), properties, ontem);

        String token = tokenService.gerar(usuario(Perfil.USER)).token();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void segredoCurtoImpedeInicializacao() {
        assertThatThrownBy(() -> new JwtProperties("curto", Duration.ofHours(8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(8)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario("Teste", "teste@teste.local", "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", 10L);
        return usuario;
    }
}
