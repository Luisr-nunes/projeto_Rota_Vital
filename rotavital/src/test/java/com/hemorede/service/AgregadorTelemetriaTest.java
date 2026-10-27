package com.hemorede.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de condição de corrida e regras de alerta 2–6 °C no agregador de telemetria (PI2-169).
 * Verifica que N threads gravando leituras simultaneamente não perdem contagem
 * nem alertas por veículo, e valida a detecção de alerta fora de 2–6 °C.
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

        AgregadorTelemetria.ResumoVeiculo resumo = agregador.obterResumoVeiculo(veiculoId).orElseThrow();
        assertEquals(4.0, resumo.menorTemperaturaC(), 1e-6);
        assertEquals(7.5, resumo.maiorTemperaturaC(), 1e-6);
        assertEquals(5.75, resumo.mediaTemperaturaC(), 1e-4);
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
        assertEquals(quantidadeVeiculos, agregador.getQuantidadeVeiculosMonitorados());
    }

    @ParameterizedTest(name = "temperatura {0} °C → fora de 2–6 °C = {1}, status = {2}")
    @CsvSource({
            "2.0,  false, OK",
            "4.0,  false, OK",
            "6.0,  false, OK",
            "1.99, true,  ALERTA",
            "0.5,  true,  ALERTA",
            "-2.0, true,  ALERTA",
            "6.01, true,  ALERTA",
            "7.8,  true,  ALERTA"
    })
    @DisplayName("Regra de alerta fora da faixa de 2 °C a 6 °C (limites inclusos)")
    void deveDetectarAlertaForaDe2a6Graus(double temperaturaC, boolean alertaEsperado, String statusEsperado) {
        AgregadorTelemetria agregador = new AgregadorTelemetria();
        boolean alerta = agregador.registrarLeitura(10L, temperaturaC);

        assertEquals(alertaEsperado, alerta);
        assertEquals(statusEsperado, AgregadorTelemetria.classificarFaixa2a6(temperaturaC));
        assertEquals(1L, agregador.getTotalLeiturasVeiculo(10L));
        assertEquals(alertaEsperado ? 1L : 0L, agregador.getTotalAlertasVeiculo(10L));
    }

    @Test
    @DisplayName("Prova comparativa: contador sem sincronização perde leituras sob N threads enquanto ConcurrentHashMap + LongAdder preserva 100%")
    void deveComprovarCondicaoDeCorridaSemSincronizacaoVsZeroPerdaComSincronizacao() throws Exception {
        int numeroDeThreads = 16;
        int leiturasPorThread = 20_000;
        long totalEsperado = (long) numeroDeThreads * leiturasPorThread;

        // Contador ingênuo (não sincronizado) sujeito a condição de corrida em leitura-modificação-escrita (count++)
        final long[] contadorSemSincronizacao = {0L};
        AgregadorTelemetria agregadorSincronizado = new AgregadorTelemetria();

        ExecutorService pool = Executors.newFixedThreadPool(numeroDeThreads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>(numeroDeThreads);

        for (int t = 0; t < numeroDeThreads; t++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                for (int i = 0; i < leiturasPorThread; i++) {
                    contadorSemSincronizacao[0]++;
                    agregadorSincronizado.registrarLeitura(1L, 4.2);
                }
                return null;
            }));
        }

        largada.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get();
        }
        pool.shutdown();

        assertEquals(totalEsperado, agregadorSincronizado.getTotalLeituras(),
                "Agregador sincronizado (ConcurrentHashMap + LongAdder) deve registrar 100% das leituras");
        assertTrue(contadorSemSincronizacao[0] < totalEsperado,
                "Contador sem sincronização deve perder atualizações por condição de corrida, mas obteve: "
                        + contadorSemSincronizacao[0]);
    }
}
