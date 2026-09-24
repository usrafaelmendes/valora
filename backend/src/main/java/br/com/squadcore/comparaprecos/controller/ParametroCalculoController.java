package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.ParametroCalculoRequest;
import br.com.squadcore.comparaprecos.dto.ParametroCalculoResponse;
import br.com.squadcore.comparaprecos.service.ParametroCalculoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Parâmetros gerais do cálculo. Consulta liberada a qualquer usuário autenticado;
 * alteração restrita ao ADMIN pelo SecurityConfig. Não há criação nem exclusão:
 * as chaves são criadas pela migration.
 */
@RestController
@RequestMapping("/parametros-calculo")
public class ParametroCalculoController {

    private final ParametroCalculoService parametroService;

    public ParametroCalculoController(ParametroCalculoService parametroService) {
        this.parametroService = parametroService;
    }

    @GetMapping
    public List<ParametroCalculoResponse> listar() {
        return parametroService.listar().stream().map(ParametroCalculoResponse::de).toList();
    }

    @GetMapping("/{chave}")
    public ParametroCalculoResponse buscar(@PathVariable String chave) {
        return ParametroCalculoResponse.de(parametroService.buscar(chave));
    }

    @PutMapping("/{chave}")
    public ParametroCalculoResponse atualizar(@PathVariable String chave,
                                              @Valid @RequestBody ParametroCalculoRequest request) {
        return ParametroCalculoResponse.de(parametroService.atualizar(chave, request.valor()));
    }
}
