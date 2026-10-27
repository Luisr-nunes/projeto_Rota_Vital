package com.hemorede.service;

import com.hemorede.service.AgregadorTelemetria.Leitura;
import com.hemorede.service.AgregadorTelemetria.ResumoVeiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Benchmark comparativo sequencial vs concorrente para o {@link AgregadorTelemetria} (PI2-169).
 * Mede tempo de execução (ms), throughput (leituras/segundo) e speedup, além de
 * validar que todas as execuções concorrentes produzem exatamente as mesmas
 * contagens e alertas da execução sequencial.
 */
class AgregadorTelemetriaBenchmarkTest {

    private static final int TOTAL_LEITURAS = 500_000;
    private static final int QUANTIDADE_VEICULOS = 20;
    private static final int RODADAS_AQUECIMENTO = 3;
    private static final int RODADAS_MEDICAO = 5;

    public record ResultadoMedicao(
            String modo,
            int threads,
            int totalLeituras,
            double tempoMedianoMs,
            double throughputLeiturasPorSegundo,
            double speedup
    ) {
    }

    @Test
    @DisplayName("Benchmark sequencial vs concorrente (tempo, throughput e paridade de resultados)")
    void deveMedirDesempenhoSequencialVsConcorrenteMantendoCorretude() {
        List<Leitura> lote = gerarLoteDeterministico(TOTAL_LEITURAS, QUANTIDADE_VEICULOS, 42L);

        // Referência sequencial para validação de corretude
        AgregadorTelemetria referencia = new AgregadorTelemetria();
        referencia.processarSequencial(lote);
        long leiturasEsperadas = referencia.getTotalLeituras();
        long alertasEsperados = referencia.getTotalAlertas();
        Map<Long, ResumoVeiculo> resumosReferencia = referencia.obterResumoPorVeiculo();

        assertEquals(TOTAL_LEITURAS, leiturasEsperadas);
        assertTrue(alertasEsperados > 0, "O lote sintético deve conter leituras de alerta fora de 2–6 °C");

        // Aquecimento da JVM (JIT)
        for (int i = 0; i < RODADAS_AQUECIMENTO; i++) {
            AgregadorTelemetria warmupSeq = new AgregadorTelemetria();
            warmupSeq.processarSequencial(lote);
            AgregadorTelemetria warmupConc = new AgregadorTelemetria();
            warmupConc.processarConcorrente(lote, 4);
        }

        double tempoSequencialMs = medirMedianaMs(() -> {
            AgregadorTelemetria ag = new AgregadorTelemetria();
            ag.processarSequencial(lote);
            assertEquals(leiturasEsperadas, ag.getTotalLeituras());
            assertEquals(alertasEsperados, ag.getTotalAlertas());
        });
        double throughputSequencial = (TOTAL_LEITURAS / (tempoSequencialMs / 1000.0));

        List<ResultadoMedicao> resultados = new ArrayList<>();
        resultados.add(new ResultadoMedicao(
                "Sequencial", 1, TOTAL_LEITURAS, tempoSequencialMs, throughputSequencial, 1.00));

        int[] configuracoesThreads = {2, 4, 8, 16};
        for (int threads : configuracoesThreads) {
            double tempoConcorrenteMs = medirMedianaMs(() -> {
                AgregadorTelemetria ag = new AgregadorTelemetria();
                ag.processarConcorrente(lote, threads);

                assertEquals(leiturasEsperadas, ag.getTotalLeituras(),
                        "Total de leituras divergente com " + threads + " threads");
                assertEquals(alertasEsperados, ag.getTotalAlertas(),
                        "Total de alertas divergente com " + threads + " threads");

                for (Map.Entry<Long, ResumoVeiculo> entry : resumosReferencia.entrySet()) {
                    ResumoVeiculo ref = entry.getValue();
                    ResumoVeiculo conc = ag.obterResumoVeiculo(entry.getKey()).orElseThrow();
                    assertEquals(ref.totalLeituras(), conc.totalLeituras());
                    assertEquals(ref.totalAlertas(), conc.totalAlertas());
                    assertEquals(ref.menorTemperaturaC(), conc.menorTemperaturaC(), 1e-6);
                    assertEquals(ref.maiorTemperaturaC(), conc.maiorTemperaturaC(), 1e-6);
                    assertEquals(ref.mediaTemperaturaC(), conc.mediaTemperaturaC(), 1e-3);
                }
            });

            double throughputConcorrente = (TOTAL_LEITURAS / (tempoConcorrenteMs / 1000.0));
            double speedup = tempoSequencialMs / tempoConcorrenteMs;
            resultados.add(new ResultadoMedicao(
                    "Concorrente", threads, TOTAL_LEITURAS, tempoConcorrenteMs, throughputConcorrente, speedup));
        }

        System.out.println("\n=== BENCHMARK TELEMETRIA (PI2-169): SEQUENCIAL vs CONCORRENTE ===");
        System.out.println("Processadores disponíveis (JVM): " + Runtime.getRuntime().availableProcessors());
        System.out.println("| Modo | Threads | Leituras | Tempo Mediano (ms) | Throughput (leituras/s) | Speedup |");
        System.out.println("|---|---:|---:|---:|---:|---:|");
        for (ResultadoMedicao r : resultados) {
            System.out.printf(Locale.US,
                    "| %s | %d | %,d | %.2f | %,.0f | %.2fx |%n",
                    r.modo(),
                    r.threads(),
                    r.totalLeituras(),
                    r.tempoMedianoMs(),
                    r.throughputLeiturasPorSegundo(),
                    r.speedup());
        }
        System.out.println("=================================================================\n");

        for (ResultadoMedicao r : resultados) {
            assertTrue(r.tempoMedianoMs() > 0.0, "Tempo medido deve ser positivo");
            assertTrue(r.throughputLeiturasPorSegundo() > 0.0, "Throughput deve ser positivo");
        }
    }

    private static double medirMedianaMs(Runnable tarefa) {
        long[] temposNs = new long[RODADAS_MEDICAO];
        for (int i = 0; i < RODADAS_MEDICAO; i++) {
            long inicio = System.nanoTime();
            tarefa.run();
            temposNs[i] = System.nanoTime() - inicio;
        }
        Arrays.sort(temposNs);
        return temposNs[RODADAS_MEDICAO / 2] / 1_000_000.0;
    }

    private static List<Leitura> gerarLoteDeterministico(int total, int veiculos, long semente) {
        Random random = new Random(semente);
        List<Leitura> leituras = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            long veiculoId = (i % veiculos) + 1L;
            double temp = Math.round((4.0 + random.nextGaussian() * 1.5) * 10.0) / 10.0;
            leituras.add(new Leitura(veiculoId, temp));
        }
        return leituras;
    }
}
