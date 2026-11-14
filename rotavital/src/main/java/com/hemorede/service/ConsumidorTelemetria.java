package com.hemorede.service;

import com.hemorede.dto.TelemetriaLeituraDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidores da fila de telemetria (PI2-168): N threads fixas bloqueiam em
 * {@code take()} e entregam cada leitura ao {@link AgregadorTelemetria}.
 */
@Component
public class ConsumidorTelemetria {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorTelemetria.class);

    private final BlockingQueue<TelemetriaLeituraDTO> fila;
    private final AgregadorTelemetria agregador;
    private final MetricasTelemetria metricas;
    private final int threads;
    private ExecutorService executor;

    public ConsumidorTelemetria(BlockingQueue<TelemetriaLeituraDTO> fila,
                                AgregadorTelemetria agregador,
                                MetricasTelemetria metricas,
                                @Value("${hemorede.telemetria.consumidores:2}") int threads) {
        this.fila = fila;
        this.agregador = agregador;
        this.metricas = metricas;
        this.threads = threads;
    }

    @PostConstruct
    void iniciar() {
        AtomicInteger sequencia = new AtomicInteger();
        executor = Executors.newFixedThreadPool(threads, tarefa -> {
            Thread thread = new Thread(tarefa, "telemetria-consumidor-" + sequencia.incrementAndGet());
            thread.setDaemon(true);   // não impede o JVM de encerrar
            return thread;
        });
        for (int i = 0; i < threads; i++) {
            executor.submit(this::consumir);
        }
    }

    private void consumir() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                TelemetriaLeituraDTO leitura = fila.take();
                agregador.registrarLeitura(leitura.veiculoId(), leitura.temperaturaC());
                metricas.registrarProcessada(leitura.timestamp());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // encerrando
            } catch (RuntimeException e) {
                log.error("Falha ao processar leitura de telemetria; seguindo para a próxima", e);
            }
        }
    }

    @PreDestroy
    void parar() {
        executor.shutdownNow();   // interrompe os take() bloqueados
    }
}