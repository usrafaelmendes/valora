package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.ComparacaoResponse;
import br.com.squadcore.comparaprecos.dto.ComparacaoResumoResponse;
import br.com.squadcore.comparaprecos.dto.CotacaoRequest;
import br.com.squadcore.comparaprecos.dto.CotacaoResponse;
import br.com.squadcore.comparaprecos.dto.CotacaoResumoResponse;
import br.com.squadcore.comparaprecos.dto.OpcaoCotacaoRequest;
import br.com.squadcore.comparaprecos.dto.OpcaoCotacaoResponse;
import br.com.squadcore.comparaprecos.service.ComparacaoService;
import br.com.squadcore.comparaprecos.service.CotacaoService;
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
 * Cotações e comparações (RF08, RF10 a RF12). Permitido a qualquer usuário autenticado
 * (REQUISITOS §3.1). Cotações, opções e comparações são históricas: não há alteração nem
 * exclusão pela API; cada nova comparação gera um novo registro.
 */
@RestController
@RequestMapping("/cotacoes")
public class CotacaoController {

    private final CotacaoService cotacaoService;
    private final ComparacaoService comparacaoService;

    public CotacaoController(CotacaoService cotacaoService, ComparacaoService comparacaoService) {
        this.cotacaoService = cotacaoService;
        this.comparacaoService = comparacaoService;
    }

    /** Cria a cotação e executa a primeira comparação (CT23). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CotacaoResponse criar(@Valid @RequestBody CotacaoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return CotacaoResponse.de(cotacaoService.criar(request, usuarioId(jwt)));
    }

    @GetMapping
    public List<CotacaoResumoResponse> listar(@RequestParam(required = false) Long produtoId) {
        return cotacaoService.listar(produtoId).stream().map(CotacaoResumoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public CotacaoResponse buscar(@PathVariable Long id) {
        return CotacaoResponse.de(cotacaoService.buscar(id));
    }

    /** Inclui uma opção; ela entra na próxima comparação. */
    @PostMapping("/{id}/opcoes")
    @ResponseStatus(HttpStatus.CREATED)
    public OpcaoCotacaoResponse adicionarOpcao(@PathVariable Long id, @Valid @RequestBody OpcaoCotacaoRequest request) {
        return OpcaoCotacaoResponse.de(cotacaoService.adicionarOpcao(id, request));
    }

    /** Nova comparação com a configuração vigente; as anteriores continuam no histórico. */
    @PostMapping("/{id}/comparacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ComparacaoResponse comparar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ComparacaoResponse.de(comparacaoService.executar(id, usuarioId(jwt)));
    }

    @GetMapping("/{id}/comparacoes")
    public List<ComparacaoResumoResponse> listarComparacoes(@PathVariable Long id) {
        return comparacaoService.listarDaCotacao(id).stream().map(ComparacaoResumoResponse::de).toList();
    }

    private static Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
