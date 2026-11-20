package com.hemorede.exception;

/** Recurso de outro hospital/doador: respondido como 404 para não revelar que existe. */
public class AcessoNegadoRecursoException extends RuntimeException {
    public AcessoNegadoRecursoException(String mensagem) {
        super(mensagem);
    }
}
