package br.com.squadcore.comparaprecos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuração do JWT, lida de variáveis de ambiente (JWT_SECRET, JWT_EXPIRACAO).
 * O segredo nunca fica no código; a aplicação não inicia sem ele.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiracao) {

    /** HS256 exige chave de pelo menos 256 bits. */
    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    public JwtProperties {
        // Falha já na inicialização: sem um segredo válido nenhum token poderia ser emitido ou verificado.
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado ou muito curto: informe pelo menos "
                            + TAMANHO_MINIMO_SEGREDO_BYTES + " caracteres no .env.");
        }
        if (expiracao == null || expiracao.isNegative() || expiracao.isZero()) {
            throw new IllegalStateException("JWT_EXPIRACAO deve ser uma duração positiva (ex.: 8h).");
        }
    }
}
