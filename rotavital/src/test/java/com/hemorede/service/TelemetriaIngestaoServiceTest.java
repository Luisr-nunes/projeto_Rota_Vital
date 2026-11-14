package com.hemorede.service;

import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.exception.FilaTelemetriaCheiaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelemetriaIngestaoServiceTest {

    private final BlockingQueue<TelemetriaLeituraDTO> fila = new LinkedBlockingQueue<>(1);
    private final MetricasTelemetria metricas = new MetricasTelemetria();
    private final TelemetriaIngestaoService service = new TelemetriaIngestaoService(fila, metricas);

    @Test
    @DisplayName("Enfileira a leitura recebida")
    void deveEnfileirar() {
        TelemetriaLeituraDTO leitura = new TelemetriaLeituraDTO(1L, 4.0, null);

        service.receber(leitura);

        assertEquals(1, fila.size());
        assertSame(leitura, fila.peek());
    }

    @Test
    @DisplayName("Fila cheia → FilaTelemetriaCheiaException, sem bloquear")
    void deveFalharComFilaCheia() {
        service.receber(new TelemetriaLeituraDTO(1L, 4.0, null));

        assertThrows(FilaTelemetriaCheiaException.class,
                () -> service.receber(new TelemetriaLeituraDTO(2L, 5.0, null)));
    }

    @Test
    @DisplayName("Leitura recusada por fila cheia é contada como perda no painel de rede (PI2-198)")
    void deveContarDescarte() {
        service.receber(new TelemetriaLeituraDTO(1L, 4.0, null));

        assertThrows(FilaTelemetriaCheiaException.class,
                () -> service.receber(new TelemetriaLeituraDTO(2L, 5.0, null)));

        assertEquals(1, metricas.resumo().leiturasDescartadas());
    }
}