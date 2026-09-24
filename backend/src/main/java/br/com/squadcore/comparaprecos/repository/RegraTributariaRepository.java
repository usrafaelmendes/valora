package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.RegraTributaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegraTributariaRepository extends JpaRepository<RegraTributaria, Long> {

    /** Inclui as desativadas: o ADMIN precisa vê-las para configurar e ativar. */
    List<RegraTributaria> findAllByOrderByTributoAscPrioridadeDescNomeAscIdAsc();

    /** Regras consideradas na seleção das regras aplicáveis a uma operação. */
    List<RegraTributaria> findByAtivaTrue();

    /** Considera também as desativadas, como o índice único do banco. */
    Optional<RegraTributaria> findByNomeIgnoreCase(String nome);
}
