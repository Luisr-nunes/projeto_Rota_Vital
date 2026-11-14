package com.hemorede.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vazão e atraso da telemetria no painel de rede (PI2-198 / N1).
 */
class MetricasTelemetriaTest {

    /** Relógio que o teste avança à mão. */
    private static final class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-11-12T12:00:00Z");

        void avancarMs(long ms) {
            agora = agora.plusMillis(ms);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final MetricasTelemetria metricas = new MetricasTelemetria(relogio);

    private OffsetDateTime haMs(long ms) {
        return OffsetDateTime.ofInstant(relogio.instant().minusMillis(ms), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Sem leituras → vazão zero e atraso nulo, sem inventar valores")
    void semLeituras() {
        MetricasTelemetria.Resumo r = metricas.resumo();

        assertEquals(0, r.leiturasProcessadas());
        assertEquals(0, r.leiturasDescartadas());
        assertEquals(0.0, r.leiturasPorSegundo());
        assertNull(r.atrasoMedioMs());
        assertNull(r.atrasoMaximoMs());
    }

    @Test
    @DisplayName("Atraso = fim do processamento − timestamp do veículo; média e máximo da janela")
    void calculaAtraso() {
        metricas.registrarProcessada(haMs(20));
        metricas.registrarProcessada(haMs(40));
        metricas.registrarProcessada(haMs(90));

        MetricasTelemetria.Resumo r = metricas.resumo();

        assertEquals(3, r.leiturasProcessadas());
        assertEquals(50.0, r.atrasoMedioMs());
        assertEquals(90.0, r.atrasoMaximoMs());
    }

    @Test
    @DisplayName("Leituras/s = leituras da janela de 10 s ÷ 10")
    void calculaVazaoNaJanela() {
        for (int i = 0; i < 50; i++) {
            metricas.registrarProcessada(haMs(5));
        }

        assertEquals(5.0, metricas.resumo().leiturasPorSegundo());
    }

    @Test
    @DisplayName("Leituras com mais de 10 s saem da janela: vazão volta a zero, total acumulado fica")
    void janelaDesliza() {
        metricas.registrarProcessada(haMs(5));
        relogio.avancarMs(10_001);

        MetricasTelemetria.Resumo r = metricas.resumo();

        assertEquals(0.0, r.leiturasPorSegundo());
        assertNull(r.atrasoMedioMs());
        assertEquals(1, r.leiturasProcessadas());
    }

    @Test
    @DisplayName("Leitura sem timestamp conta na vazão, mas não no atraso")
    void semTimestamp() {
        metricas.registrarProcessada(null);
        metricas.registrarProcessada(haMs(30));

        MetricasTelemetria.Resumo r = metricas.resumo();

        assertEquals(0.2, r.leiturasPorSegundo());
        assertEquals(30.0, r.atrasoMedioMs());
    }

    @Test
    @DisplayName("Relógio do veículo adiantado não gera atraso negativo")
    void atrasoNuncaNegativo() {
        metricas.registrarProcessada(haMs(-500));

        assertEquals(0.0, metricas.resumo().atrasoMedioMs());
    }

    @Test
    @DisplayName("Descartes por fila cheia são contados como perda")
    void contaDescartes() {
        metricas.registrarDescartada();
        metricas.registrarDescartada();

        assertEquals(2, metricas.resumo().leiturasDescartadas());
    }

    @Test
    @DisplayName("Várias threads registrando ao mesmo tempo: nenhuma leitura se perde")
    void concorrenciaSemPerda() throws InterruptedException {
        MetricasTelemetria real = new MetricasTelemetria();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int t = 0; t < 8; t++) {
            pool.submit(() -> {
                for (int i = 0; i < 1_000; i++) {
                    real.registrarProcessada(OffsetDateTime.now(ZoneOffset.UTC));
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        MetricasTelemetria.Resumo r = real.resumo();
        assertEquals(8_000, r.leiturasProcessadas());
        assertEquals(800.0, r.leiturasPorSegundo());
    }
}
