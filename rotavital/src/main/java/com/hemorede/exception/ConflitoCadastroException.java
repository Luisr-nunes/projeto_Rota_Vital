package com.hemorede.exception;

/** E-mail ou CPF já cadastrado - HTTP 409. */
public class ConflitoCadastroException extends RuntimeException {
    public ConflitoCadastroException(String mensagem) {
        super(mensagem);
    }
}
