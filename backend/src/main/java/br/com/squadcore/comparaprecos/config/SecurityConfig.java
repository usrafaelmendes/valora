package br.com.squadcore.comparaprecos.config;

import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.service.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Autenticação stateless por JWT (Bearer) e autorização por perfil (RF01, RF02).
 *
 * - POST /auth/login é público;
 * - /auth/configuracao-inicial é público (a criação do primeiro ADMIN só é aceita enquanto
 *   não existe nenhum usuário; a regra fica no ConfiguracaoInicialService);
 * - /usuarios/** exige ADMIN;
 * - /fornecedores: consulta (GET) para qualquer autenticado; criação, alteração e desativação exigem ADMIN;
 * - /produtos: consulta (GET) para qualquer autenticado; criação, alteração e desativação exigem ADMIN;
 * - /regras-tributarias e /parametros-calculo: consulta (GET) para qualquer autenticado;
 *   criação, alteração, desativação e conferência de regras aplicáveis exigem ADMIN;
 * - /calculos: executar e consultar cálculos para qualquer autenticado (ADMIN ou USER);
 * - /cotacoes e /comparacoes: criar cotações, incluir opções, executar e consultar comparações
 *   para qualquer autenticado (ADMIN ou USER);
 * - todo o resto exige usuário autenticado (ADMIN ou USER).
 *
 * CORS: somente as origens de app.cors.origens-permitidas (frontend desktop/Tauri) podem chamar a
 * API diretamente; o token vai no header Authorization, sem cookies (allowCredentials=false).
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        http
                // API stateless com token no header: não há cookie de sessão a proteger contra CSRF.
                .csrf(csrf -> csrf.disable())
                // Usa o bean corsConfigurationSource; o preflight (OPTIONS) é respondido antes da autorização.
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/configuracao-inicial").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/configuracao-inicial").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/usuarios/**").hasRole(Perfil.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/fornecedores/**").authenticated()
                        .requestMatchers("/fornecedores/**").hasRole(Perfil.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/produtos/**").authenticated()
                        .requestMatchers("/produtos/**").hasRole(Perfil.ADMIN.name())
                        .requestMatchers("/nfe/**").hasRole(Perfil.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/regras-tributarias/**").authenticated()
                        .requestMatchers("/regras-tributarias/**").hasRole(Perfil.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/parametros-calculo/**").authenticated()
                        .requestMatchers("/parametros-calculo/**").hasRole(Perfil.ADMIN.name())
                        .requestMatchers("/calculos/**").authenticated()
                        .requestMatchers("/cotacoes/**", "/comparacoes/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(properties.origensPermitidas());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Nome do arquivo do download CSV; sem isso o frontend desktop não consegue lê-lo.
        configuracao.setExposedHeaders(List.of("Content-Disposition"));
        configuracao.setAllowCredentials(false);
        configuracao.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracao);
        return source;
    }

    /** Algoritmo BCrypt, com salt aleatório embutido em cada hash (RNF05). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(properties)));
    }

    /** Exige assinatura HS256 com o mesmo segredo usado na emissão dos tokens. */
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withSecretKey(chave(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Converte a claim "perfil" do token na autoridade ROLE_ADMIN ou ROLE_USER. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static SecretKey chave(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
