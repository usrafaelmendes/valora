package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.CalculoCusto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CalculoCustoRepository extends JpaRepository<CalculoCusto, Long> {

    /** Listagem resumida (sem créditos e pendências), do mais recente para o mais antigo. */
    List<CalculoCusto> findAllByOrderByIdDesc();

    /** Detalhe com créditos e pendências. */
    @EntityGraph(attributePaths = {"creditos", "pendencias"})
    Optional<CalculoCusto> findDetalhadoById(Long id);

    /** Detalhe de vários cálculos (ex.: os de uma comparação). */
    @EntityGraph(attributePaths = {"creditos", "pendencias"})
    List<CalculoCusto> findDetalhadosByIdIn(Collection<Long> ids);
}
