package com.hemorede.controller;

import com.hemorede.exception.GlobalExceptionHandler;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.IndicadoresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaxaDescarteControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        BolsaRepository repo = mock(BolsaRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        mvc = MockMvcBuilders.standaloneSetup(new IndicadoresController(
                new IndicadoresService(repo, mock(RequisicaoRepository.class), null)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void retornaVazioPorPadraoNaJanelaDeSeteDias() throws Exception {
        mvc.perform(get("/api/v1/indicadores/descarte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("diasAnalisados").value(7))
                .andExpect(jsonPath("taxas.length()").value(0));
    }

    @Test
    void rejeitaPeriodoNaoPositivo() throws Exception {
        mvc.perform(get("/api/v1/indicadores/descarte").param("dias", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mantemAliasDaApiAntiga() throws Exception {
        mvc.perform(get("/api/indicadores/descarte").param("dias", "7"))
                .andExpect(status().isOk());
    }
}
