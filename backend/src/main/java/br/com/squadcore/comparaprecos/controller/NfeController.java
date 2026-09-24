package br.com.squadcore.comparaprecos.controller;

import br.com.squadcore.comparaprecos.dto.NfeResponse;
import br.com.squadcore.comparaprecos.dto.NfeResumoResponse;
import br.com.squadcore.comparaprecos.exception.NfeInvalidaException;
import br.com.squadcore.comparaprecos.service.NfeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * NF-e (RF05 a RF07). Importação e consulta restritas ao ADMIN pelo SecurityConfig
 * (REQUISITOS §3.2); o tamanho máximo do arquivo é definido no application.yml.
 */
@RestController
@RequestMapping("/nfe")
public class NfeController {

    /** Tipos declarados aceitos; o conteúdo é sempre validado pelo parser, independentemente do tipo. */
    private static final List<MediaType> TIPOS_ACEITOS = List.of(
            MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_OCTET_STREAM);

    private final NfeService nfeService;

    public NfeController(NfeService nfeService) {
        this.nfeService = nfeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NfeResponse importar(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        if (!tipoAceito(arquivo.getContentType())) {
            throw new NfeInvalidaException("Tipo de arquivo não aceito. Envie o XML da NF-e.");
        }
        return NfeResponse.de(nfeService.importar(arquivo.getBytes()));
    }

    @GetMapping
    public List<NfeResumoResponse> listar() {
        return nfeService.listar().stream().map(NfeResumoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public NfeResponse buscar(@PathVariable Long id) {
        return NfeResponse.de(nfeService.buscar(id));
    }

    /** Tipo não informado é aceito, pois nem todo cliente o envia; o parser valida o conteúdo. */
    private static boolean tipoAceito(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return true;
        }
        try {
            MediaType informado = MediaType.parseMediaType(tipo);
            return TIPOS_ACEITOS.stream().anyMatch(aceito -> aceito.equalsTypeAndSubtype(informado));
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }
}
