package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.exception.EmailJaCadastradoException;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    /** Limites de tamanho da senha; o máximo é o limite de bytes considerado pelo BCrypt. */
    public static final int SENHA_MIN = 8;
    public static final int SENHA_MAX = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Cria o usuário guardando somente o hash BCrypt da senha (RNF05). */
    @Transactional
    public Usuario criar(String nome, String email, String senha, Perfil perfil) {
        String emailNormalizado = normalizarEmail(email);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new EmailJaCadastradoException();
        }
        Usuario usuario = new Usuario(nome.trim(), emailNormalizado, passwordEncoder.encode(senha), perfil);
        return usuarioRepository.save(usuario);
    }

    /** Ordenados por id, incluindo usuários inativos. */
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarAtivoPorId(Long id) {
        return usuarioRepository.findById(id).filter(Usuario::isAtivo);
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
