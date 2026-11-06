package com.hemorede.exception;

/** Operação incompatível com o status atual do recurso (HTTP 409). */
public class TransicaoStatusInvalidaException extends RuntimeException {
    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}
