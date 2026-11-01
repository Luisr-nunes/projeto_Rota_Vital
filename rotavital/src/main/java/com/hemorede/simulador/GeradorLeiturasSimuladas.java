package com.hemorede.simulador;

import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.service.TelemetriaService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class GeradorLeiturasSimuladas {

    private static final double DESVIO_PADRAO_C = 1.2;
    private static final TipoRefrigeracao[] TIPOS = TipoRefrigeracao.values();

    private final Random random;
    private final Clock clock;

    public GeradorLeiturasSimuladas(Random random, Clock clock) {
        this.random = random;
        this.clock = clock;
    }

    /** Ids 1..N; o seed do sistema usa 1, 2 e 3. */
    public List<Long> idsVeiculos(int quantidade) {
        return IntStream.rangeClosed(1, quantidade).mapToObj(Long::valueOf).toList();
    }

    /** Ciclo 1→REFRIGERADO, 2→CONGELADO, 3→AMBIENTE (igual ao DataInitializer). */
    public TipoRefrigeracao tipoDoVeiculo(long veiculoId) {
        return TIPOS[(int) ((veiculoId - 1) % TIPOS.length)];
    }

    public LeituraTemperaturaResponse gerar(long veiculoId) {
        TipoRefrigeracao tipo = tipoDoVeiculo(veiculoId);
        double centro = (TelemetriaService.minimoSeguro(tipo) + TelemetriaService.maximoSeguro(tipo)) / 2.0;
        double temperatura = Math.round((centro + random.nextGaussian() * DESVIO_PADRAO_C) * 10.0) / 10.0;
        String timestamp = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new LeituraTemperaturaResponse(
                veiculoId, temperatura, timestamp, TelemetriaService.classificar(tipo, temperatura));
    }
}