package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.FornecedorRequest;
import br.com.squadcore.comparaprecos.dto.FornecedorResponse;
import br.com.squadcore.comparaprecos.service.FornecedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fornecedores (RF03). Consulta liberada a qualquer usuário autenticado;
 * criação, alteração e desativação restritas ao ADMIN pelo SecurityConfig.
 */
@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @GetMapping
    public List<FornecedorResponse> listar() {
        return fornecedorService.listarAtivos().stream().map(FornecedorResponse::de).toList();
    }

    @GetMapping("/{id}")
    public FornecedorResponse buscar(@PathVariable Long id) {
        return FornecedorResponse.de(fornecedorService.buscarAtivo(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResponse criar(@Valid @RequestBody FornecedorRequest request) {
        return FornecedorResponse.de(fornecedorService.criar(request));
    }

    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest request) {
        return FornecedorResponse.de(fornecedorService.atualizar(id, request));
    }

    /** Desativação lógica: o fornecedor não é apagado do banco. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        fornecedorService.desativar(id);
    }
}
