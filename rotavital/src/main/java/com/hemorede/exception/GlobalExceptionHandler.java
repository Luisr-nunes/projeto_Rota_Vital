package com.hemorede.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            IncompatibilidadeSanguineaException.class,
            EstoqueInsuficienteException.class,
            DoacaoForaDoIntervaloException.class,
            VeiculoIncompativelException.class
    })
    public ResponseEntity<Map<String, Object>> handleRegraNegocio(RuntimeException ex) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "erro", ex.getClass().getSimpleName(),
                "mensagem", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}
