package com.hemorede.exception;

/**
 * Lançada quando não existe caminho alcançável no grafo de rotas entre o
 * Hemocentro e o hospital solicitante (HU05 - Planejar rota de entrega).
 */
public class RotaIndisponivelException extends RuntimeException {
    public RotaIndisponivelException(String message) {
        super(message);
    }
}
