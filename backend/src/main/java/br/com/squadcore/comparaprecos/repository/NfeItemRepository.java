package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.NfeItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NfeItemRepository extends JpaRepository<NfeItem, Long> {

    /** Item com a nota, o fornecedor emitente e o produto vinculado. */
    @EntityGraph(attributePaths = {"nfe", "nfe.fornecedor", "produto"})
    Optional<NfeItem> findDetalhadoById(Long id);

    /** Itens do mesmo fornecedor e produto, da NF-e mais recente para a mais antiga. */
    @Query("""
            select i from NfeItem i join fetch i.nfe n
            where n.fornecedor.id = :fornecedorId and i.produto.id = :produtoId
            order by n.dataEmissao desc, n.id desc, i.numeroItem asc
            """)
    List<NfeItem> findDoFornecedorEProdutoMaisRecentes(@Param("fornecedorId") Long fornecedorId,
                                                      @Param("produtoId") Long produtoId);
}
