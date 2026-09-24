package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.Nfe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NfeRepository extends JpaRepository<Nfe, Long> {

    boolean existsByChaveAcesso(String chaveAcesso);

    /** Listagem resumida: carrega o fornecedor, mas não os itens. */
    @EntityGraph(attributePaths = "fornecedor")
    List<Nfe> findAllByOrderByDataEmissaoDescIdDesc();

    /** Detalhe: nota, fornecedor, itens e produtos vinculados em uma única consulta. */
    @EntityGraph(attributePaths = {"fornecedor", "itens", "itens.produto"})
    Optional<Nfe> findDetalhadaById(Long id);
}
