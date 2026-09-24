package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.ConfiguracaoInicialRequest;
import br.com.squadcore.comparaprecos.dto.ConfiguracaoInicialStatusResponse;
import br.com.squadcore.comparaprecos.dto.LoginRequest;
import br.com.squadcore.comparaprecos.dto.LoginResponse;
import br.com.squadcore.comparaprecos.dto.UsuarioResponse;
import br.com.squadcore.comparaprecos.exception.CredenciaisInvalidasException;
import br.com.squadcore.comparaprecos.service.AuthService;
import br.com.squadcore.comparaprecos.service.ConfiguracaoInicialService;
import br.com.squadcore.comparaprecos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final ConfiguracaoInicialService configuracaoInicialService;

    public AuthController(AuthService authService, UsuarioService usuarioService,
                          ConfiguracaoInicialService configuracaoInicialService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.configuracaoInicialService = configuracaoInicialService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResultado resultado = authService.login(request.email(), request.senha());
        return LoginResponse.bearer(
                resultado.token().token(),
                resultado.token().expiraEm(),
                UsuarioResponse.de(resultado.usuario()));
    }

    /** Dados do usuário autenticado; permite ao frontend saber o perfil logado. */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.buscarAtivoPorId(Long.valueOf(jwt.getSubject()))
                .map(UsuarioResponse::de)
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    /** Público: permite ao frontend saber se ainda é preciso criar o primeiro ADMIN. */
    @GetMapping("/configuracao-inicial")
    public ConfiguracaoInicialStatusResponse statusConfiguracaoInicial() {
        return new ConfiguracaoInicialStatusResponse(configuracaoInicialService.estaConfigurado());
    }

    /** Público, mas só funciona enquanto não existe nenhum usuário; depois responde 409. */
    @PostMapping("/configuracao-inicial")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse configurar(@Valid @RequestBody ConfiguracaoInicialRequest request) {
        return UsuarioResponse.de(configuracaoInicialService.criarPrimeiroAdmin(
                request.nome(), request.email(), request.senha(), request.confirmacaoSenha()));
    }
}
