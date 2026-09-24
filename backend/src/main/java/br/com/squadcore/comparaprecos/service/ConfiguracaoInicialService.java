package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.Perfil;
import br.com.squadcore.comparaprecos.entity.Usuario;
import br.com.squadcore.comparaprecos.exception.CampoInvalidoException;
import br.com.squadcore.comparaprecos.exception.SistemaJaConfiguradoException;
import br.com.squadcore.comparaprecos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primeiro acesso: enquanto não existe nenhum usuário, permite criar o primeiro ADMIN pela
 * interface. Depois disso, a criação de usuários segue somente por POST /usuarios (ADMIN).
 */
@Service
public class ConfiguracaoInicialService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public ConfiguracaoInicialService(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    /** O sistema está configurado assim que existe qualquer usuário (ativo ou não). */
    @Transactional(readOnly = true)
    public boolean estaConfigurado() {
        return usuarioRepository.count() > 0;
    }

    /**
     * Cria o primeiro ADMIN. O bloqueio da tabela usuario até o fim da transação faz uma segunda
     * requisição simultânea esperar e, em seguida, encontrar o ADMIN já criado.
     */
    @Transactional
    public Usuario criarPrimeiroAdmin(String nome, String email, String senha, String confirmacaoSenha) {
        if (!senha.equals(confirmacaoSenha)) {
            throw new CampoInvalidoException("confirmacaoSenha", "A confirmação da senha não confere.");
        }
        usuarioRepository.bloquearTabela();
        if (usuarioRepository.count() > 0) {
            throw new SistemaJaConfiguradoException();
        }
        return usuarioService.criar(nome, email, senha, Perfil.ADMIN);
    }
}
