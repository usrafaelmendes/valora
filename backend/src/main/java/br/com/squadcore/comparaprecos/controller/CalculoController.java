package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.CalculoRequest;
import br.com.squadcore.comparaprecos.dto.CalculoResponse;
import br.com.squadcore.comparaprecos.dto.CalculoResumoResponse;
import br.com.squadcore.comparaprecos.service.CalculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cálculos de custo efetivo (RF09). Executar e consultar é permitido a qualquer usuário
 * autenticado (REQUISITOS §3.1); regras e parâmetros continuam alteráveis só pelo ADMIN.
 * Um cálculo é histórico: não há alteração nem exclusão pela API.
 */
@RestController
@RequestMapping("/calculos")
public class CalculoController {

    private final CalculoService calculoService;

    public CalculoController(CalculoService calculoService) {
        this.calculoService = calculoService;
    }

    /** Sempre grava o cálculo: 201 com status CALCULADO ou INCOMPLETO (com as pendências). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalculoResponse calcular(@Valid @RequestBody CalculoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return CalculoResponse.de(calculoService.calcular(request, Long.valueOf(jwt.getSubject())));
    }

    @GetMapping
    public List<CalculoResumoResponse> listar(@RequestParam(required = false) Long fornecedorId,
                                              @RequestParam(required = false) Long produtoId) {
        return calculoService.listar(fornecedorId, produtoId).stream().map(CalculoResumoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public CalculoResponse buscar(@PathVariable Long id) {
        return CalculoResponse.de(calculoService.buscar(id));
    }
}
