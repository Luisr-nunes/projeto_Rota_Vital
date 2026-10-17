package com.hemorede.service;

import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Canal de telemetria de temperatura da cadeia fria (PI2-121), conforme o
 * contrato de {@code doc/telemetria.md}.
 * <p>
 * Na U1 não há sensor físico: a leitura é simulada pela própria aplicação,
 * com temperatura gerada em torno do centro da faixa segura do tipo de
 * refrigeração do veículo (distribuição normal, desvio de 1,2 °C). Assim,
 * a maior parte das leituras sai {@code OK} e algumas saem {@code ALERTA},
 * o que permite demonstrar a regra no painel. Na U2 a leitura passará a vir
 * do broker MQTT (porta 1883/8883).
 * </p>
 */
@Service
public class TelemetriaService {

    static final String OK = "OK";
    static final String ALERTA = "ALERTA";
    private static final double DESVIO_PADRAO_C = 1.2;

    private final VeiculoRepository veiculoRepository;
    private final Random random;
    private final Clock clock;

    @Autowired
    public TelemetriaService(VeiculoRepository veiculoRepository) {
        this(veiculoRepository, new Random(), Clock.system(ZoneId.of("America/Recife")));
    }

    /** Construtor para testes: permite fixar a semente e o relógio. */
    TelemetriaService(VeiculoRepository veiculoRepository, Random random, Clock clock) {
        this.veiculoRepository = veiculoRepository;
        this.random = random;
        this.clock = clock;
    }

    /**
     * Última leitura (simulada) de temperatura do veículo.
     *
     * @throws IllegalArgumentException se o veículo não existir (404)
     */
    public LeituraTemperaturaResponse lerTemperatura(Long veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado: " + veiculoId));

        TipoRefrigeracao tipo = veiculo.getTipoRefrigeracao();
        double centro = (minimoSeguro(tipo) + maximoSeguro(tipo)) / 2.0;
        double temperatura = Math.round((centro + random.nextGaussian() * DESVIO_PADRAO_C) * 10.0) / 10.0;
        String timestamp = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new LeituraTemperaturaResponse(veiculo.getId(), temperatura, timestamp, classificar(tipo, temperatura));
    }

    /** {@code OK} se a temperatura está na faixa segura do tipo (limites inclusos); senão {@code ALERTA}. */
    public static String classificar(TipoRefrigeracao tipo, double temperaturaC) {
        boolean dentro = temperaturaC >= minimoSeguro(tipo) && temperaturaC <= maximoSeguro(tipo);
        return dentro ? OK : ALERTA;
    }

    static double minimoSeguro(TipoRefrigeracao tipo) {
        return switch (tipo) {
            case REFRIGERADO_2_6 -> 2.0;
            case CONGELADO_MENOS_20 -> -30.0;
            case AMBIENTE_CONTROLADO_20_24 -> 20.0;
        };
    }

    static double maximoSeguro(TipoRefrigeracao tipo) {
        return switch (tipo) {
            case REFRIGERADO_2_6 -> 6.0;
            case CONGELADO_MENOS_20 -> -20.0;
            case AMBIENTE_CONTROLADO_20_24 -> 24.0;
        };
    }
}
