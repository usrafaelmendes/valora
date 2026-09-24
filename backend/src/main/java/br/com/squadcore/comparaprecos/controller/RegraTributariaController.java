package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.RegraTributariaRequest;
import br.com.squadcore.comparaprecos.dto.RegraTributariaResponse;
import br.com.squadcore.comparaprecos.dto.RegrasAplicaveisRequest;
import br.com.squadcore.comparaprecos.dto.RegrasAplicaveisResponse;
import br.com.squadcore.comparaprecos.entity.Tributo;
import br.com.squadcore.comparaprecos.service.RegraTributariaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Regras tributárias configuráveis. Consulta liberada a qualquer usuário autenticado
 * (critérios do cálculo, REQUISITOS §3.1); criação, alteração, desativação e conferência
 * das regras aplicáveis restritas ao ADMIN pelo SecurityConfig (REQUISITOS §3.2).
 */
@RestController
@RequestMapping("/regras-tributarias")
public class RegraTributariaController {

    private final RegraTributariaService regraService;

    public RegraTributariaController(RegraTributariaService regraService) {
        this.regraService = regraService;
    }

    @GetMapping
    public List<RegraTributariaResponse> listar(@RequestParam(required = false) Boolean ativa,
                                                @RequestParam(required = false) Tributo tributo) {
        return regraService.listar(ativa, tributo).stream().map(RegraTributariaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public RegraTributariaResponse buscar(@PathVariable Long id) {
        return RegraTributariaResponse.de(regraService.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegraTributariaResponse criar(@Valid @RequestBody RegraTributariaRequest request) {
        return RegraTributariaResponse.de(regraService.criar(request));
    }

    @PutMapping("/{id}")
    public RegraTributariaResponse atualizar(@PathVariable Long id, @Valid @RequestBody RegraTributariaRequest request) {
        return RegraTributariaResponse.de(regraService.atualizar(id, request));
    }

    /** Desativação lógica: a regra não é apagada do banco. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        regraService.desativar(id);
    }

    /** Mostra quais regras ativas seriam usadas em uma operação de exemplo (sem calcular créditos). */
    @PostMapping("/aplicaveis")
    public RegrasAplicaveisResponse aplicaveis(@Valid @RequestBody RegrasAplicaveisRequest request) {
        RegraTributariaService.RegrasSelecionadas selecao = regraService.regrasAplicaveis(request);
        return RegrasAplicaveisResponse.de(selecao.operacao(), selecao.tributos());
    }
}
