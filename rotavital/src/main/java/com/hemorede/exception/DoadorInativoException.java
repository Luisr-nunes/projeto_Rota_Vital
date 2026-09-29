package com.hemorede.exception;

/**
 * Indica que uma doação foi solicitada para um doador inativo.
 */
public class DoadorInativoException extends RuntimeException {

    public DoadorInativoException(String mensagem) {
        super(mensagem);
    }
}
