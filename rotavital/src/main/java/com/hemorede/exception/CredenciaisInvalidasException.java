package com.hemorede.exception;

/** Login recusado - HTTP 401. A mensagem é a mesma para e-mail inexistente e senha errada. */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos.");
    }
}
