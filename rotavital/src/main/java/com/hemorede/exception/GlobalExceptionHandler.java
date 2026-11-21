package com.hemorede.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            IncompatibilidadeSanguineaException.class,
            EstoqueInsuficienteException.class,
            DoadorInativoException.class,
            DoacaoForaDoIntervaloException.class,
            DoadorInaptoException.class,
            VeiculoIncompativelException.class,
            NenhumaBolsaCompativelException.class,
            RotaIndisponivelException.class
    })
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(RuntimeException ex) {
        return corpoErro(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(EstoqueCompativelInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleEstoqueCompativelInsuficiente(
            EstoqueCompativelInsuficienteException ex) {
        Map<String, Object> body = new LinkedHashMap<>(corpoErro(HttpStatus.CONFLICT, ex).getBody());
        body.put("quantidadeNecessaria", ex.getQuantidadeNecessaria());
        body.put("quantidadeDisponivel", ex.getQuantidadeDisponivel());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleTransicaoInvalida(TransicaoStatusInvalidaException ex) {
        return corpoErro(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(DiasPrevisaoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleDiasPrevisao(DiasPrevisaoInvalidoException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ConflitoCadastroException.class)
    public ResponseEntity<Map<String, Object>> handleConflitoCadastro(ConflitoCadastroException ex) {
        return corpoErro(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredenciais(CredenciaisInvalidasException ex) {
        return corpoErro(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Map<String, Object>> handleMuitasTentativas(MuitasTentativasException ex) {
        return corpoErro(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(DadosBolsaInvalidosException.class)
    public ResponseEntity<Map<String, Object>> handleDadosBolsa(DadosBolsaInvalidosException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, ex);
    }

    // Recurso de outro tenant (hospital/doador): 404, igual a inexistente.
    @ExceptionHandler(AcessoNegadoRecursoException.class)
    public ResponseEntity<Map<String, Object>> handleOutroTenant(AcessoNegadoRecursoException ex) {
        return corpoErro(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(FiltroValidadeInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleFiltroValidade(FiltroValidadeInvalidoException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ModeloProbabilisticoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleModeloPendente(ModeloProbabilisticoIndisponivelException ex) {
        return corpoErro(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    // IllegalArgumentException é usada nos controllers/services para "recurso não encontrado".
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(IllegalArgumentException ex) {
        return corpoErro(HttpStatus.NOT_FOUND, ex);
    }

    // IllegalStateException é usada para requisições em um estado que não permite a operação.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleEstadoInvalido(IllegalStateException ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, ex);
    }

    // Parâmetro de query/path com valor que não corresponde ao tipo esperado
    // (ex.: ?tipo=INVALIDO para um enum) ou parâmetro obrigatório ausente.
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> handleParametroInvalido(Exception ex) {
        return corpoErro(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(FilaTelemetriaCheiaException.class)
    public ResponseEntity<Map<String, Object>> handleFilaCheia(FilaTelemetriaCheiaException ex) {
        return corpoErro(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    // Corpo inválido (Bean Validation nos DTOs de entrada): 400 com a mensagem de cada campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("erro", "DadosInvalidos");
        body.put("mensagem", "Confira os dados informados:");
        body.put("erros", erros);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // JSON malformado, data fora do formato ou valor de enum desconhecido no corpo.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        String mensagem = "O corpo da requisição não está em JSON válido.";
        if (ex.getCause() instanceof MismatchedInputException mi && !mi.getPath().isEmpty()) {
            String campo = mi.getPath().get(mi.getPath().size() - 1).getFieldName();
            mensagem = mi instanceof InvalidFormatException
                    ? "Valor inválido no campo '" + campo + "'. Confira o formato (datas em aaaa-mm-dd) e as opções aceitas."
                    : "Tipo de dado inválido no campo '" + campo + "'.";
        }
        return corpoErro(HttpStatus.BAD_REQUEST, "CorpoInvalido", mensagem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMetodo(HttpRequestMethodNotSupportedException ex) {
        return corpoErro(HttpStatus.METHOD_NOT_ALLOWED, "MetodoNaoSuportado",
                "O método " + ex.getMethod() + " não é aceito neste endereço.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMidia(HttpMediaTypeNotSupportedException ex) {
        return corpoErro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FormatoNaoSuportado",
                "Envie os dados como JSON (Content-Type: application/json).");
    }

    // Unicidade no banco (ex.: dois cadastros simultâneos com o mesmo CPF ou e-mail).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return corpoErro(HttpStatus.CONFLICT, "ConflitoDeDados",
                "Já existe um registro com esses dados (CPF, e-mail ou código). Confira e tente de novo.");
    }

    private ResponseEntity<Map<String, Object>> corpoErro(HttpStatus status, Exception ex) {
        return corpoErro(status, ex.getClass().getSimpleName(), ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> corpoErro(HttpStatus status, String erro, String mensagem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("erro", erro);
        body.put("mensagem", mensagem);
        return ResponseEntity.status(status).body(body);
    }
}
