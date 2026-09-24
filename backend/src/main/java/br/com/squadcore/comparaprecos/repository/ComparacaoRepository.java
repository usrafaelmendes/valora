package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.Comparacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComparacaoRepository extends JpaRepository<Comparacao, Long> {

    /** Histórico resumido da cotação, da mais recente para a mais antiga. */
    List<Comparacao> findByCotacaoIdOrderByIdDesc(Long cotacaoId);

    Optional<Comparacao> findFirstByCotacaoIdOrderByIdDesc(Long cotacaoId);

    /** Detalhe com a configuração usada e os resultados. */
    @EntityGraph(attributePaths = {"parametros", "regras", "resultados"})
    Optional<Comparacao> findDetalhadaById(Long id);
}
