package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByNomeAscIdAsc();

    Optional<Produto> findByIdAndAtivoTrue(Long id);

    /** Considera também os desativados, como os índices únicos do banco. */
    Optional<Produto> findByNomeIgnoreCase(String nome);

    Optional<Produto> findByGtin(String gtin);
}
