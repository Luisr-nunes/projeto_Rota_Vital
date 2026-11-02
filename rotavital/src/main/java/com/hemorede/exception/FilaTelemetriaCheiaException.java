package com.hemorede.exception;

public class FilaTelemetriaCheiaException extends RuntimeException {
    public FilaTelemetriaCheiaException(int capacidade) {
        super("Fila de telemetria cheia (capacidade " + capacidade + "); tente novamente em instantes");
    }
}