package com.hemorede.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste de condição de corrida no agregador de telemetria (PI2-169).
 * Verifica que N threads gravando leituras simultaneamente não perdem contagem
 * nem alertas por veículo.
 */
class AgregadorTelemetriaTest {

    @Test
    @DisplayName("N threads gravando leituras no mesmo veículo não perdem contagem")
    void nThreadsGravandoNoMesmoVeiculoNaoPerdemContagem() throws Exception {
        AgregadorTelemetria agregador = new AgregadorTelemetria();
        int numeroDeThreads = 16;
        int leiturasPorThread = 10_000;
        long totalEsperado = (long) numeroDeThreads * leiturasPorThread;
        long veiculoId = 1L;

        ExecutorService pool = Executors.newFixedThreadPool(numeroDeThreads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Callable<Void>> tarefas = new ArrayList<>(numeroDeThreads);

        for (int t = 0; t < numeroDeThreads; t++) {
            tarefas.add(() -> {
                largada.await();
                for (int i = 0; i < leiturasPorThread; i++) {
                    // Alterna entre leitura normal (4.0 °C) e alerta (7.5 °C)
                    double temp = (i % 2 == 0) ? 4.0 : 7.5;
                    agregador.registrarLeitura(veiculoId, temp);
                }
                return null;
            });
        }

        List<Future<Void>> futuros = new ArrayList<>(numeroDeThreads);
        for (Callable<Void> tarefa : tarefas) {
            futuros.add(pool.submit(tarefa));
        }
        largada.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get();
        }
        pool.shutdown();

        assertEquals(totalEsperado, agregador.getTotalLeituras(),
                "Total global de leituras não pode sofrer perda por condição de corrida");
        assertEquals(totalEsperado, agregador.getTotalLeiturasVeiculo(veiculoId),
                "Total de leituras do veículo não pode sofrer perda por condição de corrida");
        assertEquals(totalEsperado / 2, agregador.getTotalAlertasVeiculo(veiculoId),
                "Total de alertas fora de 2–6 °C não pode sofrer perda por condição de corrida");
    }

    @Test
    @DisplayName("N threads gravando leituras distribuídas entre múltiplos veículos mantêm soma exata")
    void nThreadsGravandoEmMultiplosVeiculosNaoPerdemContagem() throws Exception {
        AgregadorTelemetria agregador = new AgregadorTelemetria();
        int numeroDeThreads = 16;
        int leiturasPorThread = 8_000;
        int quantidadeVeiculos = 8;
        long totalEsperado = (long) numeroDeThreads * leiturasPorThread;

        ExecutorService pool = Executors.newFixedThreadPool(numeroDeThreads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>(numeroDeThreads);

        for (int t = 0; t < numeroDeThreads; t++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                for (int i = 0; i < leiturasPorThread; i++) {
                    long veiculoId = (i % quantidadeVeiculos) + 1L;
                    double temp = (i % 4 == 0) ? 1.2 : 4.5;
                    agregador.registrarLeitura(veiculoId, temp);
                }
                return null;
            }));
        }

        largada.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get();
        }
        pool.shutdown();

        long somaPorVeiculos = 0;
        for (long v = 1; v <= quantidadeVeiculos; v++) {
            somaPorVeiculos += agregador.getTotalLeiturasVeiculo(v);
        }

        assertEquals(totalEsperado, agregador.getTotalLeituras());
        assertEquals(totalEsperado, somaPorVeiculos);
        assertEquals(totalEsperado / 4, agregador.getTotalAlertas());
    }
}
