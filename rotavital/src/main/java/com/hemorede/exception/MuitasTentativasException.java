package com.hemorede.exception;

/** Login bloqueado temporariamente após falhas seguidas (429). */
public class MuitasTentativasException extends RuntimeException {
    public MuitasTentativasException(String mensagem) {
        super(mensagem);
    }
}
