package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.CriarUsuarioRequest;
import br.com.squadcore.comparaprecos.dto.UsuarioResponse;
import br.com.squadcore.comparaprecos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gerenciamento de usuários: restrito ao ADMIN pelo SecurityConfig. */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return UsuarioResponse.de(
                usuarioService.criar(request.nome(), request.email(), request.senha(), request.perfil()));
    }
}
