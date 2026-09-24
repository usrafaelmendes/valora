package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.exception.SistemaJaConfiguradoException;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Criação do primeiro ADMIN com requisições simultâneas contra o PostgreSQL real.
 *
 * Usa um banco temporário exclusivo, criado por este teste e removido ao fim da JVM, no PostgreSQL do
 * compose.yaml (credenciais do .env da raiz), como os testes E2E: o banco de desenvolvimento
 * nunca é usado. Sem PostgreSQL acessível, o teste é ignorado. Os dados são fictícios.
 */
@SpringBootTest
@DirtiesContext
@EnabledIf("postgresDisponivel")
class ConfiguracaoInicialConcorrenciaTest {

    private static final int REQUISICOES = 8;
    private static final String SENHA_TESTE = "senha-de-teste-123";
    private static final String BANCO = "compara_precos_teste_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private ConfiguracaoInicialService configuracaoInicialService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @DynamicPropertySource
    static void bancoTemporario(DynamicPropertyRegistry registry) throws SQLException {
        executarNoBancoPrincipal("CREATE DATABASE " + BANCO);
        // Remove o banco ao fim da JVM, mesmo se o contexto falhar ao iniciar.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executarNoBancoPrincipal("DROP DATABASE IF EXISTS " + BANCO + " WITH (FORCE)");
            } catch (SQLException e) {
                System.err.println("Não foi possível remover o banco temporário " + BANCO + ": " + e.getMessage());
            }
        }));
        registry.add("spring.datasource.url", () -> url(BANCO));
        registry.add("spring.datasource.username", () -> ambiente().get("POSTGRES_USER"));
        registry.add("spring.datasource.password", () -> ambiente().get("POSTGRES_PASSWORD"));
        registry.add("app.jwt.secret", () -> "segredo-somente-para-testes-com-mais-de-32-caracteres");
    }

    @Test
    void requisicoesSimultaneasCriamUmUnicoAdmin() throws Exception {
        assertThat(configuracaoInicialService.estaConfigurado()).isFalse();

        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Usuario>> resultados = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES);
        try {
            for (int i = 0; i < REQUISICOES; i++) {
                String email = "admin" + i + "@teste.local";
                Callable<Usuario> tentativa = () -> {
                    largada.await();
                    return configuracaoInicialService.criarPrimeiroAdmin("Admin " + email, email, SENHA_TESTE, SENHA_TESTE);
                };
                resultados.add(executor.submit(tentativa));
            }
            largada.countDown();

            int criados = 0;
            int rejeitados = 0;
            for (Future<Usuario> resultado : resultados) {
                try {
                    resultado.get(30, TimeUnit.SECONDS);
                    criados++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(SistemaJaConfiguradoException.class);
                    rejeitados++;
                }
            }
            assertThat(criados).isEqualTo(1);
            assertThat(rejeitados).isEqualTo(REQUISICOES - 1);
        } finally {
            executor.shutdownNow();
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        assertThat(usuarios).hasSize(1);
        Usuario admin = usuarios.getFirst();
        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getSenhaHash()).isNotEqualTo(SENHA_TESTE).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches(SENHA_TESTE, admin.getSenhaHash())).isTrue();
        assertThat(configuracaoInicialService.estaConfigurado()).isTrue();
    }

    /** Condição do @EnabledIf: o PostgreSQL do compose.yaml está acessível com as credenciais do .env. */
    static boolean postgresDisponivel() {
        Map<String, String> ambiente = ambiente();
        if (ambiente.get("POSTGRES_DB") == null || ambiente.get("POSTGRES_USER") == null) {
            return false;
        }
        try (Connection ignorada = conectar(ambiente.get("POSTGRES_DB"))) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void executarNoBancoPrincipal(String sql) throws SQLException {
        try (Connection conexao = conectar(ambiente().get("POSTGRES_DB"));
             Statement comando = conexao.createStatement()) {
            comando.execute(sql);
        }
    }

    private static Connection conectar(String banco) throws SQLException {
        Map<String, String> ambiente = ambiente();
        return DriverManager.getConnection(url(banco), ambiente.get("POSTGRES_USER"), ambiente.get("POSTGRES_PASSWORD"));
    }

    private static String url(String banco) {
        Map<String, String> ambiente = ambiente();
        return "jdbc:postgresql://" + ambiente.getOrDefault("POSTGRES_HOST", "localhost") + ":"
                + ambiente.getOrDefault("POSTGRES_PORT", "5432") + "/" + banco;
    }

    /** Variáveis do .env da raiz; as variáveis de ambiente do sistema têm prioridade, como no application.yml. */
    private static Map<String, String> ambiente() {
        Map<String, String> valores = new HashMap<>();
        for (Path arquivo : List.of(Path.of("../.env"), Path.of(".env"))) {
            if (Files.isRegularFile(arquivo)) {
                try {
                    for (String linha : Files.readAllLines(arquivo)) {
                        int igual = linha.indexOf('=');
                        if (!linha.isBlank() && !linha.trim().startsWith("#") && igual > 0) {
                            valores.putIfAbsent(linha.substring(0, igual).trim(), linha.substring(igual + 1).trim());
                        }
                    }
                } catch (IOException e) {
                    // Sem .env legível: valem somente as variáveis de ambiente do sistema.
                }
            }
        }
        valores.putAll(System.getenv());
        return valores;
    }
}
