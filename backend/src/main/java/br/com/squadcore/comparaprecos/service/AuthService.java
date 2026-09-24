package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.exception.CredenciaisInvalidasException;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

/**
 * Login com e-mail e senha (CT01/CT02).
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
    }

    public LoginResultado login(String email, String senha) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), senha));
        } catch (AuthenticationException e) {
            // Mesma resposta para e-mail inexistente, senha errada ou usuário inativo,
            // para não revelar quais e-mails estão cadastrados.
            throw new CredenciaisInvalidasException();
        }

        // Busca o usuário já autenticado para montar o token e a resposta.
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(CredenciaisInvalidasException::new);
        return new LoginResultado(usuario, tokenService.gerar(usuario));
    }

    /** Agrupa o usuário autenticado e o token emitido para o controller montar a resposta. */
    public record LoginResultado(Usuario usuario, TokenService.TokenGerado token) {
    }
}
