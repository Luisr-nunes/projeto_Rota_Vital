package com.hemorede.controller;

import com.hemorede.service.AgregadorTelemetria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato do POST /api/v1/telemetria/leituras (PI2-168): 202 com a leitura
 * indo para a fila e para o agregador; 400 sem veiculoId ou sem temperatura (PI2-30).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TelemetriaLeituraControllerIntegrationTest {

    private static final String URL = "/api/v1/telemetria/leituras";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgregadorTelemetria agregador;

    @Test
    @DisplayName("Leitura válida → 202 e a leitura chega ao agregador pelo consumidor")
    void deveAceitarLeituraValida() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"veiculoId": 910001, "temperaturaC": 4.5,
                                 "timestamp": "2026-11-02T09:00:00-03:00"}
                                """))
                .andExpect(status().isAccepted());

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertEquals(1L, agregador.getTotalLeiturasVeiculo(910001L)));
    }

    @Test
    @DisplayName("Sem veiculoId → 400 apontando o campo")
    void deveRetornar400SemVeiculoId() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temperaturaC\": 4.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.veiculoId").value("veiculoId é obrigatório"));
    }

    @Test
    @DisplayName("Sem temperaturaC → 400 apontando o campo")
    void deveRetornar400SemTemperatura() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"veiculoId\": 910002}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.temperaturaC").value("temperaturaC é obrigatória"));
    }
}