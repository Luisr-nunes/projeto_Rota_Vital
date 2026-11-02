package com.hemorede.simulador;

import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.service.TelemetriaIngestaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimuladorTelemetriaTest {

    private SimuladorTelemetria simulador(BlockingQueue<TelemetriaLeituraDTO> fila, int veiculos) {
        SimuladorProperties props = new SimuladorProperties(true, CenarioCarga.MODERADO,
                Map.of(CenarioCarga.MODERADO, new SimuladorProperties.Cenario(veiculos, 1000)));
        return new SimuladorTelemetria(props, new TelemetriaIngestaoService(fila), null);
    }

    @Test
    @DisplayName("Uma rodada envia uma leitura válida por veículo do cenário para a fila")
    void enviaUmaLeituraPorVeiculo() {
        BlockingQueue<TelemetriaLeituraDTO> fila = new ArrayBlockingQueue<>(100);

        simulador(fila, 5).enviarRodada();

        List<Long> ids = fila.stream().map(TelemetriaLeituraDTO::veiculoId).toList();
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), ids);
        fila.forEach(l -> {
            assertNotNull(l.temperaturaC());
            assertNotNull(l.timestamp());
        });
    }

    @Test
    @DisplayName("Fila cheia não derruba o simulador: descarta o resto da rodada")
    void filaCheiaNaoLancaExcecao() {
        BlockingQueue<TelemetriaLeituraDTO> fila = new ArrayBlockingQueue<>(2);

        simulador(fila, 5).enviarRodada();

        assertEquals(2, fila.size());
    }
}
