package com.hemorede.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            IncompatibilidadeSanguineaException.class,
            EstoqueInsuficienteException.class,
            DoacaoForaDoIntervaloException.class,
            VeiculoIncompativelException.class,
            RotaIndisponivelException.class
    })
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(RuntimeException ex) {
        return corpoErro(HttpStatus.UNPROCESSABLE_ENTITY, ex);
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

    private ResponseEntity<Map<String, Object>> corpoErro(HttpStatus status, Exception ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "erro", ex.getClass().getSimpleName(),
                "mensagem", ex.getMessage()
        );
        return ResponseEntity.status(status).body(body);
    }
}
