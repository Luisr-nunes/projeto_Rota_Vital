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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrevisaoDemandaControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        BolsaRepository bolsas = mock(BolsaRepository.class);
        RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
        when(bolsas.findAll()).thenReturn(List.of());
        when(requisicoes.findAll()).thenReturn(List.of());
        ModeloProbabilisticoDemanda modelo = new ModeloProbabilisticoDemanda();
        var lambdas = new EnumMap<TipoSanguineo, Double>(TipoSanguineo.class);
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            lambdas.put(tipo, 0.25);
        }
        modelo.setLambdaDiario(lambdas);
        mvc = MockMvcBuilders.standaloneSetup(new IndicadoresController(
                new IndicadoresService(bolsas, requisicoes, modelo)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void retornaPrevisaoPorTipoEUsaSeteDiasComoPadrao() throws Exception {
        mvc.perform(get("/api/v1/indicadores/previsao-demanda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("diasPrevistos").value(7))
                .andExpect(jsonPath("janelaHistoricaDias").value(7))
                .andExpect(jsonPath("previsoes.length()").value(8))
                .andExpect(jsonPath("previsoes[0].tipoSanguineo").isNotEmpty())
                .andExpect(jsonPath("previsoes[0].origem").value("LAMBDA_DE_REFERENCIA_PI2_177"));
    }

    @Test
    void rejeitaHorizonteInvalido() throws Exception {
        mvc.perform(get("/api/v1/indicadores/previsao-demanda").param("dias", "8"))
                .andExpect(status().isBadRequest());
    }
}
