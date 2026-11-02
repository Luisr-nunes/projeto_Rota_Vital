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
    private final TelemetriaIngestaoService service = new TelemetriaIngestaoService(fila);

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
}