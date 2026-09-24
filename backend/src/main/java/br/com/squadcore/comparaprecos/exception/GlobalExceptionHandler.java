package br.com.squadcore.comparaprecos.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento padronizado de erros da API (formato ProblemDetail, RFC 9457).
 *
 * Erros conhecidos do Spring MVC (400, 404, 405...) são tratados pela classe base.
 * Erros inesperados retornam uma mensagem genérica, sem expor detalhes internos
 * (RF15); o detalhe completo fica apenas no log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ProblemDetail handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SistemaJaConfiguradoException.class)
    public ProblemDetail handleSistemaJaConfigurado(SistemaJaConfiguradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(FornecedorNaoEncontradoException.class)
    public ProblemDetail handleFornecedorNaoEncontrado(FornecedorNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CnpjJaCadastradoException.class)
    public ProblemDetail handleCnpjJaCadastrado(CnpjJaCadastradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ProblemDetail handleProdutoNaoEncontrado(ProdutoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProdutoJaCadastradoException.class)
    public ProblemDetail handleProdutoJaCadastrado(ProdutoJaCadastradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NfeInvalidaException.class)
    public ProblemDetail handleNfeInvalida(NfeInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NfeJaImportadaException.class)
    public ProblemDetail handleNfeJaImportada(NfeJaImportadaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NfeNaoEncontradaException.class)
    public ProblemDetail handleNfeNaoEncontrada(NfeNaoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** NF-e válida, mas o emitente não é um fornecedor ativo: situação de negócio, não erro de formato. */
    @ExceptionHandler(FornecedorDaNfeNaoCadastradoException.class)
    public ProblemDetail handleFornecedorDaNfeNaoCadastrado(FornecedorDaNfeNaoCadastradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(RegraTributariaNaoEncontradaException.class)
    public ProblemDetail handleRegraTributariaNaoEncontrada(RegraTributariaNaoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegraTributariaJaCadastradaException.class)
    public ProblemDetail handleRegraTributariaJaCadastrada(RegraTributariaJaCadastradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ParametroNaoEncontradoException.class)
    public ProblemDetail handleParametroNaoEncontrado(ParametroNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CalculoNaoEncontradoException.class)
    public ProblemDetail handleCalculoNaoEncontrado(CalculoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CotacaoNaoEncontradaException.class)
    public ProblemDetail handleCotacaoNaoEncontrada(CotacaoNaoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ComparacaoNaoEncontradaException.class)
    public ProblemDetail handleComparacaoNaoEncontrada(ComparacaoNaoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Mesmo formato da validação de campos (propriedade "erros"). */
    @ExceptionHandler(CampoInvalidoException.class)
    public ProblemDetail handleCampoInvalido(CampoInvalidoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Dados inválidos. Corrija os campos informados.");
        problema.setProperty("erros", Map.of(ex.getCampo(), ex.getMessage()));
        return problema;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Você não tem permissão para acessar este recurso.");
    }

    /** Validação de entrada: informa quais campos precisam ser corrigidos (CT06, CT08, CT29). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "Dados inválidos. Corrija os campos informados.");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /**
     * JSON malformado ou valor incompatível com o tipo do campo (ex.: UF ou tipo de fornecedor
     * fora dos valores aceitos). Quando possível, informa o campo e os valores aceitos (CT06, CT29).
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problema;
        if (ex.getCause() instanceof JacksonException jackson && campo(jackson) != null) {
            problema = ProblemDetail.forStatusAndDetail(status, "Dados inválidos. Corrija os campos informados.");
            problema.setProperty("erros", Map.of(campo(jackson), mensagemValorInvalido(jackson)));
        } else {
            problema = ProblemDetail.forStatusAndDetail(status, "O corpo da requisição está ausente ou mal formatado.");
        }
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /** Upload multipart sem a parte esperada (ex.: importação de NF-e sem o campo "arquivo"). */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status,
                "O arquivo não foi enviado. Envie-o no campo \"" + ex.getRequestPartName() + "\".");
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /** Arquivo acima do limite de spring.servlet.multipart.max-file-size (413). */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status,
                "O arquivo enviado excede o tamanho máximo permitido.");
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno. Tente novamente ou contate o administrador.");
    }

    private static String campo(JacksonException ex) {
        List<JacksonException.Reference> caminho = ex.getPath();
        return caminho.isEmpty() ? null : caminho.getLast().getPropertyName();
    }

    private static String mensagemValorInvalido(JacksonException ex) {
        if (ex instanceof InvalidFormatException formato && formato.getTargetType() != null
                && formato.getTargetType().isEnum()) {
            String aceitos = Arrays.stream(formato.getTargetType().getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return "Valor inválido. Valores aceitos: " + aceitos + ".";
        }
        return "Valor inválido ou em formato incorreto.";
    }
}
