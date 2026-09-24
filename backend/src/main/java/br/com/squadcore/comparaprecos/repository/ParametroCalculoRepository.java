package br.com.squadcore.comparaprecos.repository;

import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroCalculoRepository extends JpaRepository<ParametroCalculo, ChaveParametro> {
}
