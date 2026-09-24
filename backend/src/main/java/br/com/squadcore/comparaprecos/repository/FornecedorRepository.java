package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findByAtivoTrueOrderByRazaoSocialAscIdAsc();

    Optional<Fornecedor> findByIdAndAtivoTrue(Long id);

    /** Considera também os desativados: o CNPJ continua reservado ao fornecedor original. */
    Optional<Fornecedor> findByCnpj(String cnpj);
}
