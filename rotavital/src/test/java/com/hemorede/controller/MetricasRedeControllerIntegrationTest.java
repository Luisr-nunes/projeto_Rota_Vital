package com.hemorede.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de GET /api/v1/metricas/rede (PI2-31, critério 1): latência p50/p95,
 * requisições por segundo e contagem de 4xx e 5xx.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MetricasRedeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Responde 200 com os campos do painel de rede")
    void deveRetornarMetricasAgregadas() throws Exception {
        mockMvc.perform(get("/api/v1/metricas/rede"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculadoEm").exists())
                .andExpect(jsonPath("$.totalRequisicoes").isNumber())
                .andExpect(jsonPath("$.requisicoesPorSegundo").isNumber())
                .andExpect(jsonPath("$.erros4xx").isNumber())
                .andExpect(jsonPath("$.erros5xx").isNumber())
                .andExpect(jsonPath("$.latenciaP50Ms").hasJsonPath())
                .andExpect(jsonPath("$.latenciaP95Ms").hasJsonPath());
    }
}
