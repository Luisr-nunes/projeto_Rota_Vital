package com.hemorede.exception;

/** Doador fora dos critérios de elegibilidade (idade, peso, frequência anual) - HTTP 422. */
public class DoadorInaptoException extends RuntimeException {
    public DoadorInaptoException(String mensagem) {
        super(mensagem);
    }
}
