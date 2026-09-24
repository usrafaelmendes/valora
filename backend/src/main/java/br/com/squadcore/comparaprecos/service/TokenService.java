package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.config.JwtProperties;
import br.com.squadcore.comparaprecos.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite o JWT de acesso. O "sub" é o id do usuário e a claim "perfil" define as permissões.
 */
@Service
public class TokenService {

    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_EMAIL = "email";
    /** Emissor gravado na claim "iss" de todo token. */
    private static final String EMISSOR = "compara-precos";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this(jwtEncoder, properties, Clock.systemUTC());
    }

    /** Também usado nos testes, com um Clock fixo para controlar as datas do token. */
    TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenGerado gerar(Usuario usuario) {
        // O JWT guarda datas em segundos; truncar mantém "expiraEm" igual à claim "exp".
        Instant agora = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = agora.plus(properties.expiracao());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(CLAIM_EMAIL, usuario.getEmail())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenGerado(token, expiraEm);
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }
}
