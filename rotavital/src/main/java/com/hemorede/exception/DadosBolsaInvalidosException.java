package com.hemorede.exception;

/** Datas de coleta/validade inconsistentes com o hemocomponente (RN-03) - HTTP 400. */
public class DadosBolsaInvalidosException extends RuntimeException {
    public DadosBolsaInvalidosException(String mensagem) {
        super(mensagem);
    }
}
