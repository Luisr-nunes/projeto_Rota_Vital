package com.hemorede.service;

import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.repository.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regra de alerta da telemetria (doc/telemetria.md, seção 2) — PI2-121.
 */
class TelemetriaServiceTest {

    private final VeiculoRepository veiculoRepository = mock(VeiculoRepository.class);
    private final Clock relogioFixo = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneId.of("America/Recife"));

    @ParameterizedTest(name = "{0} a {1} °C → {2}")
    @CsvSource({
            "REFRIGERADO_2_6, 2.0, OK",
            "REFRIGERADO_2_6, 6.0, OK",
            "REFRIGERADO_2_6, 4.2, OK",
            "REFRIGERADO_2_6, 1.9, ALERTA",
            "REFRIGERADO_2_6, 6.1, ALERTA",
            "CONGELADO_MENOS_20, -25.0, OK",
            "CONGELADO_MENOS_20, -19.5, ALERTA",
            "AMBIENTE_CONTROLADO_20_24, 22.0, OK",
            "AMBIENTE_CONTROLADO_20_24, 24.5, ALERTA"
    })
    @DisplayName("Classifica a leitura pela faixa segura do tipo de refrigeração (limites inclusos)")
    void deveClassificarPelaFaixaSegura(TipoRefrigeracao tipo, double temperatura, String esperado) {
        assertEquals(esperado, TelemetriaService.classificar(tipo, temperatura));
    }

    @Test
    @DisplayName("Gera leitura do veículo com timestamp ISO-8601 com fuso e status coerente com a temperatura")
    void deveGerarLeituraDoVeiculo() {
        Veiculo veiculo = new Veiculo(7L, "ROTA-7777", TipoRefrigeracao.REFRIGERADO_2_6, 200, StatusVeiculo.DISPONIVEL);
        when(veiculoRepository.findById(7L)).thenReturn(Optional.of(veiculo));
        TelemetriaService service = new TelemetriaService(veiculoRepository, new Random(42), relogioFixo);

        LeituraTemperaturaResponse leitura = service.lerTemperatura(7L);

        assertEquals(7L, leitura.veiculoId());
        assertEquals("2026-10-17T09:00:00-03:00", leitura.timestamp());
        assertTrue(leitura.temperaturaC() > -10 && leitura.temperaturaC() < 15, "temperatura perto da faixa 2–6 °C");
        assertEquals(TelemetriaService.classificar(TipoRefrigeracao.REFRIGERADO_2_6, leitura.temperaturaC()), leitura.status());
    }

    @Test
    @DisplayName("Veículo inexistente gera IllegalArgumentException (404 no handler global)")
    void deveFalharParaVeiculoInexistente() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());
        TelemetriaService service = new TelemetriaService(veiculoRepository, new Random(1), relogioFixo);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.lerTemperatura(99L));
        assertEquals("Veículo não encontrado: 99", erro.getMessage());
    }
}
