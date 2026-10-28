package com.hemorede.service;

import com.hemorede.dto.MetricasRedeResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Agrega o timer http.server.requests (Actuator/Micrometer) de todas as rotas da API.
 * O p50/p95 vem da soma dos histogramas de cada rota: percentis de rotas diferentes
 * não podem ser somados, mas as contagens por balde podem. Os baldes são os limites
 * de management.metrics.distribution.slo (application.yml); o percentil devolvido é o
 * limite superior do balde onde ele cai.
 */
@Service
public class MetricasRedeService {

    private final MeterRegistry registry;
    private final Instant inicio = Instant.now();

    public MetricasRedeService(MeterRegistry registry) {
        this.registry = registry;
    }

    public MetricasRedeResponse calcular() {
        List<Timer> timers = registry.find("http.server.requests").timers().stream()
                .filter(MetricasRedeService::trafegoDaApi)
                .toList();
        List<HistogramSnapshot> snapshots = timers.stream().map(Timer::takeSnapshot).toList();

        long total = snapshots.stream().mapToLong(HistogramSnapshot::count).sum();
        double segundos = Math.max(1, Duration.between(inicio, Instant.now()).toSeconds());
        // shortcut: vazão média desde o início do processo; trocar por janela deslizante se o painel precisar de vazão instantânea
        double rps = arredondar(total / segundos);

        return new MetricasRedeResponse(
                LocalDateTime.now(),
                total,
                rps,
                percentilMs(snapshots, total, 0.50),
                percentilMs(snapshots, total, 0.95),
                contarStatus(timers, '4'),
                contarStatus(timers, '5'));
    }

    private static boolean trafegoDaApi(Timer timer) {
        String uri = timer.getId().getTag("uri");
        // o painel consulta este endpoint a cada 10 s; contá-lo distorceria a própria medição
        return uri != null && !uri.startsWith("/actuator") && !uri.equals("/api/v1/metricas/rede");
    }

    private static long contarStatus(List<Timer> timers, char classe) {
        return timers.stream()
                .filter(t -> {
                    String status = t.getId().getTag("status");
                    return status != null && !status.isEmpty() && status.charAt(0) == classe;
                })
                .mapToLong(Timer::count)
                .sum();
    }

    private static Double percentilMs(List<HistogramSnapshot> snapshots, long total, double percentil) {
        if (total == 0) return null;

        // limite superior do balde (ns) -> requisições acumuladas até esse limite, somando todas as rotas
        Map<Double, Double> acumulado = new TreeMap<>();
        double maximoNs = 0;
        for (HistogramSnapshot s : snapshots) {
            for (CountAtBucket balde : s.histogramCounts()) {
                acumulado.merge(balde.bucket(), balde.count(), Double::sum);
            }
            maximoNs = Math.max(maximoNs, s.max(TimeUnit.NANOSECONDS));
        }

        double alvo = percentil * total;
        for (Map.Entry<Double, Double> e : acumulado.entrySet()) {
            if (e.getValue() >= alvo) return arredondar(e.getKey() / 1_000_000.0);
        }
        // amostras acima do maior balde: usa o máximo observado
        return arredondar(maximoNs / 1_000_000.0);
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
