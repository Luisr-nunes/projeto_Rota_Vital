package com.hemorede.service;

import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.exception.FilaTelemetriaCheiaException;
import com.hemorede.simulador.CenarioCarga;
import com.hemorede.simulador.SimuladorProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstração concorrente da telemetria nos 3 cenários (J4 · PI2-192).
 *
 * <p>Usa o pipeline real da aplicação (fila de 1.000 posições, consumidores e
 * {@link AgregadorTelemetria}) e uma thread por veículo, com o número de veículos de cada
 * cenário de {@code hemorede.simulador.cenarios}. Para caber no CI, o intervalo entre leituras
 * é dividido por {@link #COMPRESSAO}. Em cada cenário confere a invariante da sincronização:
 * enviadas = processadas + descartadas, e as contagens por veículo batem com o que cada
 * veículo realmente entregou à fila.
 */
class CargaConcorrenteTelemetriaTest {

    private static final int CAPACIDADE_FILA = 1000;
    private static final int CONSUMIDORES = 2;
    private static final int COMPRESSAO = 50;
    private static final int RODADAS_POR_VEICULO = 30;
    private static final int LEITURAS_POR_VEICULO_NA_SATURACAO = 2_000;

    record Resultado(String cenario, int veiculos, long intervaloEfetivoMs, long enviadas,
                     long aceitas, long processadas, long descartadas, long alertas,
                     double duracaoMs) {
        double leiturasPorSegundo() {
            return processadas / (duracaoMs / 1000.0);
        }
    }

    @Test
    @DisplayName("Cenário baixo: 10 veículos, nenhuma leitura perdida")
    void cenarioBaixo() throws Exception {
        Resultado r = executarCenario("baixo", 10, 5000, false);
        imprimir(r);
        assertSemPerda(r);
    }

    @Test
    @DisplayName("Cenário moderado: 50 veículos, nenhuma leitura perdida")
    void cenarioModerado() throws Exception {
        Resultado r = executarCenario("moderado", 50, 1000, false);
        imprimir(r);
        assertSemPerda(r);
    }

    @Test
    @DisplayName("Cenário alto: 100 veículos, nenhuma leitura perdida")
    void cenarioAlto() throws Exception {
        Resultado r = executarCenario("alto", 100, 200, false);
        imprimir(r);
        assertSemPerda(r);
    }

    @Test
    @DisplayName("Saturação (100 veículos sem pausa): fila cheia descarta, mas enviadas = processadas + descartadas")
    void saturacaoContaCadaLeituraComoProcessadaOuDescartada() throws Exception {
        Resultado r = executarCenario("saturacao", 100, 200, true);
        imprimir(r);
        assertEquals(100L * LEITURAS_POR_VEICULO_NA_SATURACAO, r.enviadas());
        assertEquals(r.enviadas(), r.processadas() + r.descartadas(),
                "Toda leitura enviada deve ter sido processada ou contada como descartada");
        assertEquals(r.aceitas(), r.processadas());
        assertTrue(r.processadas() > 0);
    }

    @Test
    @DisplayName("Os cenários padrão têm os veículos documentados (10, 50 e 100)")
    void parametrosDosCenariosDocumentados() {
        SimuladorProperties props = new SimuladorProperties(false, null, null);
        assertEquals(10, props.cenarios().get(CenarioCarga.BAIXO).veiculos());
        assertEquals(50, props.cenarios().get(CenarioCarga.MODERADO).veiculos());
        assertEquals(100, props.cenarios().get(CenarioCarga.ALTO).veiculos());
    }

    private static void assertSemPerda(Resultado r) {
        long esperado = (long) r.veiculos() * RODADAS_POR_VEICULO;
        assertEquals(esperado, r.enviadas());
        assertEquals(0, r.descartadas(), "Nesse cenário nenhuma leitura pode ser descartada");
        assertEquals(esperado, r.processadas(), "Leituras enviadas = leituras processadas");
    }

    private static Resultado executarCenario(String nome, int veiculos, long intervaloMs, boolean saturar)
            throws Exception {
        BlockingQueue<TelemetriaLeituraDTO> fila = new LinkedBlockingQueue<>(CAPACIDADE_FILA);
        MetricasTelemetria metricas = new MetricasTelemetria();
        AgregadorTelemetria agregador = new AgregadorTelemetria();
        TelemetriaIngestaoService ingestao = new TelemetriaIngestaoService(fila, metricas);
        ConsumidorTelemetria consumidor = new ConsumidorTelemetria(fila, agregador, metricas, CONSUMIDORES);
        consumidor.iniciar();

        long intervaloEfetivoMs = Math.max(1, intervaloMs / COMPRESSAO);
        int leituras = saturar ? LEITURAS_POR_VEICULO_NA_SATURACAO : RODADAS_POR_VEICULO;
        // o que cada veículo realmente entregou à fila (referência independente do agregador)
        AtomicLongArray aceitasPorVeiculo = new AtomicLongArray(veiculos + 1);
        AtomicLongArray alertasPorVeiculo = new AtomicLongArray(veiculos + 1);
        AtomicLongArray enviadasPorVeiculo = new AtomicLongArray(veiculos + 1);

        ExecutorService veiculosPool = Executors.newFixedThreadPool(veiculos);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch fim = new CountDownLatch(veiculos);
        long inicio;
        try {
            for (int v = 1; v <= veiculos; v++) {
                final int id = v;
                veiculosPool.submit(() -> {
                    try {
                        largada.await();
                        long proxima = System.nanoTime();
                        for (int i = 0; i < leituras; i++) {
                            double temp = Math.round((4.0 + ThreadLocalRandom.current().nextGaussian() * 2.0) * 10.0) / 10.0;
                            enviadasPorVeiculo.incrementAndGet(id);
                            try {
                                ingestao.receber(new TelemetriaLeituraDTO((long) id, temp, OffsetDateTime.now()));
                                aceitasPorVeiculo.incrementAndGet(id);
                                if (AgregadorTelemetria.isForaDaFaixa2a6(temp)) {
                                    alertasPorVeiculo.incrementAndGet(id);
                                }
                            } catch (FilaTelemetriaCheiaException e) {
                                // já contada como descartada pela própria ingestão
                            }
                            if (!saturar) {
                                proxima += TimeUnit.MILLISECONDS.toNanos(intervaloEfetivoMs);
                                long espera = proxima - System.nanoTime();
                                if (espera > 0) {
                                    LockSupport.parkNanos(espera);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        fim.countDown();
                    }
                });
            }
            inicio = System.nanoTime();
            largada.countDown();
            assertTrue(fim.await(60, TimeUnit.SECONDS), "Veículos não terminaram de enviar a tempo");
        } finally {
            veiculosPool.shutdownNow();
        }

        long aceitas = 0;
        long alertasEsperados = 0;
        long enviadas = 0;
        for (int v = 1; v <= veiculos; v++) {
            aceitas += aceitasPorVeiculo.get(v);
            alertasEsperados += alertasPorVeiculo.get(v);
            enviadas += enviadasPorVeiculo.get(v);
        }

        // espera os consumidores esvaziarem a fila
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (metricas.resumo().leiturasProcessadas() < aceitas && System.nanoTime() < limite) {
            Thread.sleep(1);
        }
        double duracaoMs = (System.nanoTime() - inicio) / 1_000_000.0;
        consumidor.parar();

        assertEquals(aceitas, agregador.getTotalLeituras(), "Agregador deve ter todas as leituras aceitas na fila");
        assertEquals(alertasEsperados, agregador.getTotalAlertas(), "Alertas 2–6 °C divergentes");
        for (int v = 1; v <= veiculos; v++) {
            assertEquals(aceitasPorVeiculo.get(v), agregador.getTotalLeiturasVeiculo(v), "Leituras do veículo " + v);
            assertEquals(alertasPorVeiculo.get(v), agregador.getTotalAlertasVeiculo(v), "Alertas do veículo " + v);
        }
        // na saturação um veículo pode ficar sem nenhuma leitura aceita; só os que entregaram são monitorados
        int veiculosComLeituraAceita = 0;
        for (int v = 1; v <= veiculos; v++) {
            if (aceitasPorVeiculo.get(v) > 0) {
                veiculosComLeituraAceita++;
            }
        }
        assertEquals(veiculosComLeituraAceita, agregador.getQuantidadeVeiculosMonitorados());

        MetricasTelemetria.Resumo resumo = metricas.resumo();
        assertEquals(aceitas, resumo.leiturasProcessadas());
        return new Resultado(nome, veiculos, intervaloEfetivoMs, enviadas, aceitas,
                agregador.getTotalLeituras(), resumo.leiturasDescartadas(), agregador.getTotalAlertas(), duracaoMs);
    }

    private static void imprimir(Resultado r) {
        System.out.printf(Locale.US,
                "CARGA-TELEMETRIA | %s | veiculos=%d | intervalo=%d ms | enviadas=%d | processadas=%d | descartadas=%d | alertas=%d | %.0f ms | %,.0f leituras/s%n",
                r.cenario(), r.veiculos(), r.intervaloEfetivoMs(), r.enviadas(), r.processadas(),
                r.descartadas(), r.alertas(), r.duracaoMs(), r.leiturasPorSegundo());
    }
}
