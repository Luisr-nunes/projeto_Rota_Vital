package com.hemorede.service;

import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.exception.FilaTelemetriaCheiaException;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class TelemetriaIngestaoService {

    private final BlockingQueue<TelemetriaLeituraDTO> fila;

    public TelemetriaIngestaoService(BlockingQueue<TelemetriaLeituraDTO> fila) {
        this.fila = fila;
    }

    /** Enfileira sem bloquear; fila cheia lança {@link FilaTelemetriaCheiaException}. */
    public void receber(TelemetriaLeituraDTO leitura) {
        if (!fila.offer(leitura)) {
            throw new FilaTelemetriaCheiaException(fila.size() + fila.remainingCapacity());
        }
    }
}