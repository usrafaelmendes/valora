package br.com.squadcore.comparaprecos.service;

import br.com.squadcore.comparaprecos.entity.ChaveParametro;
import br.com.squadcore.comparaprecos.entity.ParametroCalculo;
import br.com.squadcore.comparaprecos.exception.CampoInvalidoException;
import br.com.squadcore.comparaprecos.exception.ParametroNaoEncontradoException;
import br.com.squadcore.comparaprecos.repository.ParametroCalculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Parâmetros gerais do cálculo (ex.: UF de destino, arredondamento), alteráveis pelo ADMIN.
 * Os parâmetros existem desde a migration V6: a API só consulta e altera valores.
 */
@Service
public class ParametroCalculoService {

    private final ParametroCalculoRepository repository;

    public ParametroCalculoService(ParametroCalculoRepository repository) {
        this.repository = repository;
    }

    /** Na ordem de declaração de ChaveParametro. */
    @Transactional(readOnly = true)
    public List<ParametroCalculo> listar() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(ParametroCalculo::getChave))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParametroCalculo buscar(String chave) {
        return repository.findById(chave(chave)).orElseThrow(ParametroNaoEncontradoException::new);
    }

    @Transactional
    public ParametroCalculo atualizar(String chave, String valor) {
        ParametroCalculo parametro = buscar(chave);
        try {
            parametro.setValor(parametro.getChave().normalizar(valor));
        } catch (IllegalArgumentException ex) {
            throw new CampoInvalidoException("valor", ex.getMessage());
        }
        return repository.saveAndFlush(parametro);
    }

    /** Valor atual do parâmetro; vazio quando ainda não definido. */
    @Transactional(readOnly = true)
    public Optional<String> valor(ChaveParametro chave) {
        return repository.findById(chave).map(ParametroCalculo::getValor);
    }

    private static ChaveParametro chave(String chave) {
        return Arrays.stream(ChaveParametro.values())
                .filter(conhecida -> conhecida.name().equalsIgnoreCase(chave))
                .findFirst()
                .orElseThrow(ParametroNaoEncontradoException::new);
    }
}
