package com.hemorede.service;

import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.exception.FilaTelemetriaCheiaException;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class TelemetriaIngestaoService {

    private final BlockingQueue<TelemetriaLeituraDTO> fila;
    private final MetricasTelemetria metricas;

    public TelemetriaIngestaoService(BlockingQueue<TelemetriaLeituraDTO> fila, MetricasTelemetria metricas) {
        this.fila = fila;
        this.metricas = metricas;
    }

    /** Enfileira sem bloquear; fila cheia conta como perda e lança {@link FilaTelemetriaCheiaException}. */
    public void receber(TelemetriaLeituraDTO leitura) {
        if (!fila.offer(leitura)) {
            metricas.registrarDescartada();
            throw new FilaTelemetriaCheiaException(fila.size() + fila.remainingCapacity());
        }
    }
}