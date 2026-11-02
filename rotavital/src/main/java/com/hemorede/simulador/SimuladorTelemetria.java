package com.hemorede.simulador;

import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.exception.FilaTelemetriaCheiaException;
import com.hemorede.service.TelemetriaIngestaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;

/**
 * Liga o simulador (PI2-170) quando {@code hemorede.simulador.enabled=true}: a cada
 * {@code intervalo-ms} do cenário ativo, cada veículo envia uma leitura para a mesma
 * fila do POST /api/v1/telemetria/leituras (PI2-168).
 */
@Component
@ConditionalOnProperty(name = "hemorede.simulador.enabled", havingValue = "true")
public class SimuladorTelemetria implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SimuladorTelemetria.class);

    private final SimuladorProperties props;
    private final TelemetriaIngestaoService ingestao;
    private final TaskScheduler scheduler;
    private final GeradorLeiturasSimuladas gerador = new GeradorLeiturasSimuladas(new Random(), Clock.systemDefaultZone());

    public SimuladorTelemetria(SimuladorProperties props, TelemetriaIngestaoService ingestao, TaskScheduler scheduler) {
        this.props = props;
        this.ingestao = ingestao;
        this.scheduler = scheduler;
    }

    @Override
    public void run(ApplicationArguments args) {
        SimuladorProperties.Cenario cenario = props.ativo();
        log.info("Simulador de telemetria ligado: cenário {} ({} veículos a cada {} ms)",
                props.cenario(), cenario.veiculos(), cenario.intervaloMs());
        scheduler.scheduleAtFixedRate(this::enviarRodada, Duration.ofMillis(cenario.intervaloMs()));
    }

    /** Uma leitura por veículo do cenário ativo. Fila cheia descarta a leitura e segue. */
    void enviarRodada() {
        for (Long veiculoId : gerador.idsVeiculos(props.ativo().veiculos())) {
            LeituraTemperaturaResponse leitura = gerador.gerar(veiculoId);
            try {
                ingestao.receber(new TelemetriaLeituraDTO(
                        veiculoId, leitura.temperaturaC(), OffsetDateTime.parse(leitura.timestamp())));
            } catch (FilaTelemetriaCheiaException e) {
                log.warn("Simulador: {}", e.getMessage());
                return;
            }
        }
    }
}
