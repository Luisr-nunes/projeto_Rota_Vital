package com.hemorede.simulador;

import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.service.TelemetriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulador de veículos (PI2-170): leituras sintéticas válidas nos cenários
 * baixo/moderado/alto, conforme o contrato de doc/telemetria.md.
 */
class GeradorLeiturasSimuladasTest {

    private final Clock relogioFixo =
            Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("America/Recife"));
    private final GeradorLeiturasSimuladas gerador =
            new GeradorLeiturasSimuladas(new Random(42), relogioFixo);

    @ParameterizedTest(name = "{0}: {1} veículos")
    @CsvSource({"BAIXO, 10", "MODERADO, 50", "ALTO, 100"})
    @DisplayName("Gera leitura válida para cada veículo em cada cenário de carga")
    void deveGerarLeiturasValidasPorVeiculo(String cenario, int veiculos) {
        List<Long> ids = gerador.idsVeiculos(veiculos);

        assertEquals(veiculos, ids.size());
        assertEquals(veiculos, new HashSet<>(ids).size(), "ids de veículo devem ser únicos");

        for (Long id : ids) {
            LeituraTemperaturaResponse leitura = gerador.gerar(id);
            TipoRefrigeracao tipo = gerador.tipoDoVeiculo(id);

            assertEquals(id, leitura.veiculoId());
            assertEquals("2026-10-28T09:00:00-03:00", leitura.timestamp());
            OffsetDateTime.parse(leitura.timestamp()); // ISO-8601 com fuso
            assertTrue(Double.isFinite(leitura.temperaturaC()));
            assertTrue(leitura.temperaturaC() > TelemetriaService.minimoSeguro(tipo) - 8
                    && leitura.temperaturaC() < TelemetriaService.maximoSeguro(tipo) + 8,
                    "temperatura perto da faixa segura do tipo " + tipo);
            assertEquals(TelemetriaService.classificar(tipo, leitura.temperaturaC()), leitura.status());
        }
    }

    @Test
    @DisplayName("Os três primeiros veículos seguem o tipo de refrigeração do seed")
    void deveAlinharTiposComOSeed() {
        assertEquals(TipoRefrigeracao.REFRIGERADO_2_6, gerador.tipoDoVeiculo(1));
        assertEquals(TipoRefrigeracao.CONGELADO_MENOS_20, gerador.tipoDoVeiculo(2));
        assertEquals(TipoRefrigeracao.AMBIENTE_CONTROLADO_20_24, gerador.tipoDoVeiculo(3));
        assertEquals(TipoRefrigeracao.REFRIGERADO_2_6, gerador.tipoDoVeiculo(4));
    }
}