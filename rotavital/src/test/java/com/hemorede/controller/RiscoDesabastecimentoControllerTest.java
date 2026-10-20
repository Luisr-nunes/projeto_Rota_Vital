package com.hemorede.controller;

import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.exception.GlobalExceptionHandler;
import com.hemorede.indicadores.ModeloProbabilisticoDemanda;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.IndicadoresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.EnumMap;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RiscoDesabastecimentoControllerTest {
    private MockMvc mvc;
    private ModeloProbabilisticoDemanda modelo;

    @BeforeEach
    void preparar() {
        BolsaRepository bolsas = mock(BolsaRepository.class);
        RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
        when(bolsas.findAll()).thenReturn(List.of());
        when(requisicoes.findAll()).thenReturn(List.of());
        modelo = new ModeloProbabilisticoDemanda();
        mvc = MockMvcBuilders.standaloneSetup(new IndicadoresController(
                new IndicadoresService(bolsas, requisicoes, modelo)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void deveRetornar503SemModeloSemInventarProbabilidade() throws Exception {
        mvc.perform(get("/api/v1/indicadores/risco-desabastecimento").param("dias", "7"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("erro").value("ModeloProbabilisticoIndisponivelException"))
                .andExpect(jsonPath("riscos").doesNotExist());
    }

    @Test
    void deveValidarDiasAntesDeConsultarModelo() throws Exception {
        for (String dias : List.of("0", "-1", "abc")) {
            mvc.perform(get("/api/v1/indicadores/risco-desabastecimento").param("dias", dias))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void deveCalcularNasDuasUrlsComModeloConfigurado() throws Exception {
        var lambdas = new EnumMap<TipoSanguineo, Double>(TipoSanguineo.class);
        for (TipoSanguineo tipo : TipoSanguineo.values()) lambdas.put(tipo, 0.0);
        lambdas.put(TipoSanguineo.O_POS, 1.0);
        modelo.setLambdaDiario(lambdas);
        for (String base : List.of("/api/indicadores", "/api/v1/indicadores")) {
            mvc.perform(get(base + "/risco-desabastecimento").param("dias", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("riscos.length()").value(8))
                    .andExpect(jsonPath("riscos[0].tipoSanguineo").value("O_POS"))
                    .andExpect(jsonPath("riscos[0].probabilidadeDesabastecimento").value(0.950213));
        }
    }

    @Test
    void devePreservarConsultaExistenteSemModeloDeRisco() throws Exception {
        mvc.perform(get("/api/indicadores")).andExpect(status().isOk());
    }
}
