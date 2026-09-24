package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.Cotacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CotacaoRepository extends JpaRepository<Cotacao, Long> {

    /** Listagem resumida (com o produto, sem as opções), da mais recente para a mais antiga. */
    @EntityGraph(attributePaths = "produto")
    List<Cotacao> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "produto")
    List<Cotacao> findByProdutoIdOrderByIdDesc(Long produtoId);

    /** Detalhe: produto, opções e fornecedores das opções. */
    @EntityGraph(attributePaths = {"produto", "opcoes", "opcoes.fornecedor"})
    Optional<Cotacao> findDetalhadaById(Long id);
}
