package com.hemorede.controller;

import com.hemorede.exception.EstoqueCompativelInsuficienteException;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = AlocacaoController.class)
public class AlocacaoExceptionHandler {

    @ExceptionHandler(NenhumaBolsaCompativelException.class)
    public ResponseEntity<Map<String, Object>> handleSemBolsaCompativel(
            NenhumaBolsaCompativelException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, ex, Map.of());
    }

    @ExceptionHandler(EstoqueCompativelInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleEstoqueInsuficiente(
            EstoqueCompativelInsuficienteException ex) {
        return erro(HttpStatus.CONFLICT, ex, Map.of(
                "quantidadeNecessaria", ex.getQuantidadeNecessaria(),
                "quantidadeDisponivel", ex.getQuantidadeDisponivel()
        ));
    }

    private ResponseEntity<Map<String, Object>> erro(
            HttpStatus status,
            RuntimeException ex,
            Map<String, Object> detalhes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("erro", ex.getClass().getSimpleName());
        body.put("mensagem", ex.getMessage());
        body.putAll(detalhes);
        return ResponseEntity.status(status).body(body);
    }
}
