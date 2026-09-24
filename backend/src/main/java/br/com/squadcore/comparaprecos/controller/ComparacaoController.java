package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.ComparacaoResponse;
import br.com.squadcore.comparaprecos.service.ComparacaoService;
import br.com.squadcore.comparaprecos.service.ExportacaoComparacaoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** Consulta e download de uma comparação já executada (qualquer usuário autenticado). */
@RestController
@RequestMapping("/comparacoes")
public class ComparacaoController {

    /** CSV em UTF-8 (RFC 4180 usa text/csv). */
    static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ComparacaoService comparacaoService;
    private final ExportacaoComparacaoService exportacaoService;

    public ComparacaoController(ComparacaoService comparacaoService, ExportacaoComparacaoService exportacaoService) {
        this.comparacaoService = comparacaoService;
        this.exportacaoService = exportacaoService;
    }

    @GetMapping("/{id}")
    public ComparacaoResponse buscar(@PathVariable Long id) {
        return ComparacaoResponse.de(comparacaoService.buscar(id));
    }

    /** Tabela da comparação gravada, para download (RF13, CT25). Nada é recalculado. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ExportacaoComparacaoService.Arquivo arquivo = exportacaoService.exportar(id);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome()).build().toString())
                .body(arquivo.conteudo());
    }
}
