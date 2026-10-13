package com.hemorede.controller;

import com.hemorede.exception.DiasPrevisaoInvalidoException;
import com.hemorede.exception.ModeloProbabilisticoIndisponivelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = IndicadoresController.class)
public class RiscoDesabastecimentoExceptionHandler {

    @ExceptionHandler(DiasPrevisaoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleDiasInvalidos(DiasPrevisaoInvalidoException ex) {
        return erro(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ModeloProbabilisticoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleModeloPendente(
            ModeloProbabilisticoIndisponivelException ex) {
        return erro(HttpStatus.SERVICE_UNAVAILABLE, ex);
    }

    private ResponseEntity<Map<String, Object>> erro(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "erro", ex.getClass().getSimpleName(),
                "mensagem", ex.getMessage()
        ));
    }
}
