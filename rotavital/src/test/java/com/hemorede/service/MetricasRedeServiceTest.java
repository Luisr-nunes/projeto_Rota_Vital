package com.hemorede.service;

import com.hemorede.dto.MetricasRedeResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agregação de http.server.requests para GET /api/v1/metricas/rede (PI2-173).
 */
class MetricasRedeServiceTest {

    // mesmos limites de management.metrics.distribution.slo em application.yml
    private static final Duration[] BALDES = java.util.stream.Stream.of(5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000)
            .map(Duration::ofMillis).toArray(Duration[]::new);

    private static void registrar(MeterRegistry registry, String uri, String status, long millis, int vezes) {
        Timer timer = Timer.builder("http.server.requests")
                .tags("uri", uri, "status", status, "method", "GET")
                .serviceLevelObjectives(BALDES)
                .register(registry);
        for (int i = 0; i < vezes; i++) {
            timer.record(Duration.ofMillis(millis));
        }
    }

    @Test
    @DisplayName("Sem tráfego → total zero e latências nulas, sem inventar valores")
    void semTrafego() {
        MetricasRedeResponse r = new MetricasRedeService(new SimpleMeterRegistry()).calcular();

        assertEquals(0, r.totalRequisicoes());
        assertNull(r.latenciaP50Ms());
        assertNull(r.latenciaP95Ms());
        assertEquals(0, r.erros4xx());
        assertEquals(0, r.erros5xx());
        assertEquals(0.0, r.requisicoesPorSegundo());
    }

    @Test
    @DisplayName("Soma todas as URIs e status: total, 4xx, 5xx e p50/p95 do conjunto")
    void agregaTodasAsRotas() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registrar(registry, "/api/estoque", "200", 10, 90);
        registrar(registry, "/api/hospitais/{id}", "404", 10, 5);
        registrar(registry, "/api/rotas", "500", 1000, 5);

        MetricasRedeResponse r = new MetricasRedeService(registry).calcular();

        assertEquals(100, r.totalRequisicoes());
        assertEquals(5, r.erros4xx());
        assertEquals(5, r.erros5xx());
        // 95 de 100 amostras têm 10 ms: p50 e p95 caem no balde de 10 ms
        assertEquals(10.0, r.latenciaP50Ms());
        assertEquals(10.0, r.latenciaP95Ms());
        assertTrue(r.requisicoesPorSegundo() > 0);
    }

    @Test
    @DisplayName("p95 sobe quando mais de 5% das requisições são lentas")
    void p95CapturaCaudaLenta() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registrar(registry, "/api/estoque", "200", 10, 90);
        registrar(registry, "/api/rotas", "200", 1000, 10);

        MetricasRedeResponse r = new MetricasRedeService(registry).calcular();

        assertEquals(10.0, r.latenciaP50Ms());
        assertEquals(1000.0, r.latenciaP95Ms());
    }

    @Test
    @DisplayName("Latência acima do maior balde (5 s) usa o máximo observado")
    void acimaDoMaiorBalde() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registrar(registry, "/api/rotas", "200", 8000, 2);

        assertEquals(8000.0, new MetricasRedeService(registry).calcular().latenciaP95Ms());
    }

    @Test
    @DisplayName("Ignora /actuator e o próprio /api/v1/metricas/rede para o painel não medir a si mesmo")
    void ignoraTrafegoDeMonitoramento() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registrar(registry, "/actuator/health", "200", 5, 50);
        registrar(registry, "/api/v1/metricas/rede", "200", 5, 50);
        registrar(registry, "/api/estoque", "200", 20, 3);

        MetricasRedeResponse r = new MetricasRedeService(registry).calcular();

        assertEquals(3, r.totalRequisicoes());
    }
}
