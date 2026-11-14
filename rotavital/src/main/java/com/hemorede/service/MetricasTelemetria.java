package com.hemorede.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.LongAdder;

/**
 * Métricas do canal de telemetria para o painel de rede (PI2-198 / N1).
 * <p>
 * Os consumidores da fila registram cada leitura processada e o atraso entre o
 * {@code timestamp} enviado pelo veículo e o fim do processamento. A vazão e o
 * atraso são calculados numa janela deslizante de {@value #JANELA_SEGUNDOS} s, para o
 * painel mostrar o que está acontecendo agora e zerar quando o simulador para.
 * </p>
 */
@Component
public class MetricasTelemetria {

    static final int JANELA_SEGUNDOS = 10;

    public record Resumo(
            long leiturasProcessadas,
            long leiturasDescartadas,
            double leiturasPorSegundo,
            Double atrasoMedioMs,
            Double atrasoMaximoMs
    ) {
    }

    private record Amostra(long processadaEmMs, Long atrasoMs) {
    }

    private final Clock clock;
    private final LongAdder processadas = new LongAdder();
    private final LongAdder descartadas = new LongAdder();
    // protegida por synchronized: os consumidores gravam e o endpoint lê ao mesmo tempo
    private final Deque<Amostra> janela = new ArrayDeque<>();

    public MetricasTelemetria() {
        this(Clock.systemUTC());
    }

    MetricasTelemetria(Clock clock) {
        this.clock = clock;
    }

    /** Chamado pelo consumidor depois de agregar a leitura. {@code timestamp} nulo: conta, mas sem atraso. */
    public void registrarProcessada(OffsetDateTime timestamp) {
        long agora = clock.millis();
        Long atraso = timestamp == null ? null : Math.max(0, agora - timestamp.toInstant().toEpochMilli());
        processadas.increment();
        synchronized (janela) {
            janela.addLast(new Amostra(agora, atraso));
            descartarAntigas(agora);
        }
    }

    /** Leitura recusada porque a fila estava cheia: é a perda do canal de telemetria. */
    public void registrarDescartada() {
        descartadas.increment();
    }

    public Resumo resumo() {
        long agora = clock.millis();
        long naJanela = 0;
        long comAtraso = 0;
        long somaAtraso = 0;
        long maxAtraso = 0;
        synchronized (janela) {
            descartarAntigas(agora);
            for (Amostra a : janela) {
                naJanela++;
                if (a.atrasoMs() != null) {
                    comAtraso++;
                    somaAtraso += a.atrasoMs();
                    maxAtraso = Math.max(maxAtraso, a.atrasoMs());
                }
            }
        }
        return new Resumo(
                processadas.sum(),
                descartadas.sum(),
                arredondar((double) naJanela / JANELA_SEGUNDOS),
                comAtraso == 0 ? null : arredondar((double) somaAtraso / comAtraso),
                comAtraso == 0 ? null : (double) maxAtraso);
    }

    private void descartarAntigas(long agora) {
        long limite = agora - Duration.ofSeconds(JANELA_SEGUNDOS).toMillis();
        while (!janela.isEmpty() && janela.peekFirst().processadaEmMs() <= limite) {
            janela.pollFirst();
        }
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
