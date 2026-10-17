package com.hemorede.controller;

import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.repository.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato do endpoint de telemetria (doc/telemetria.md, seção 5) — PI2-121.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TelemetriaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Test
    @DisplayName("Veículo existente → 200 com veiculoId, temperaturaC, timestamp e status")
    void deveRetornarLeituraDoVeiculo() throws Exception {
        Veiculo veiculo = veiculoRepository.save(
                new Veiculo(null, "TEL-0001", TipoRefrigeracao.REFRIGERADO_2_6, 120, StatusVeiculo.DISPONIVEL));

        mockMvc.perform(get("/api/telemetria/temperatura").param("veiculoId", veiculo.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veiculoId").value(veiculo.getId()))
                .andExpect(jsonPath("$.temperaturaC").isNumber())
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-03:00")))
                .andExpect(jsonPath("$.status", anyOf(is("OK"), is("ALERTA"))));
    }

    @Test
    @DisplayName("Veículo inexistente → 404 com a mensagem do contrato")
    void deveRetornar404ParaVeiculoInexistente() throws Exception {
        mockMvc.perform(get("/api/telemetria/temperatura").param("veiculoId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("IllegalArgumentException"))
                .andExpect(jsonPath("$.mensagem").value("Veículo não encontrado: 999999"));
    }

    @Test
    @DisplayName("veiculoId ausente → 400")
    void deveRetornar400SemVeiculoId() throws Exception {
        mockMvc.perform(get("/api/telemetria/temperatura"))
                .andExpect(status().isBadRequest());
    }
}
