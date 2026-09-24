package br.com.squadcore.comparaprecos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens do frontend autorizadas a chamar a API diretamente pelo navegador/webview (CORS),
 * lidas de app.cors.origens-permitidas (variável CORS_ORIGENS_PERMITIDAS).
 *
 * A versão web usa o proxy do Vite (mesma origem); o frontend desktop (Tauri) chama o backend
 * local diretamente e por isso precisa de CORS. Somente origens exatas: sem curinga.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> origensPermitidas) {

    public CorsProperties {
        origensPermitidas = origensPermitidas == null ? List.of() : List.copyOf(origensPermitidas);
        if (origensPermitidas.stream().anyMatch(origem -> origem.contains("*"))) {
            throw new IllegalStateException("CORS_ORIGENS_PERMITIDAS não aceita curinga (*): informe origens exatas.");
        }
    }
}
