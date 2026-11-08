package com.hemorede.exception;

/**
 * Filtro de validade inválido na consulta de estoque (HU02), por exemplo
 * {@code venceEmDias} negativo. Tratada pelo {@link GlobalExceptionHandler} como HTTP 400.
 */
public class FiltroValidadeInvalidoException extends RuntimeException {
    public FiltroValidadeInvalidoException(String mensagem) {
        super(mensagem);
    }
}
