package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca pelo e-mail sem diferenciar maiúsculas/minúsculas, como o índice único do banco. */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Impede inserções por outras transações até o fim da transação atual (leituras continuam
     * permitidas). Usado na configuração inicial para que só um primeiro ADMIN seja criado.
     */
    @Modifying
    @Query(value = "LOCK TABLE usuario IN EXCLUSIVE MODE", nativeQuery = true)
    void bloquearTabela();
}
