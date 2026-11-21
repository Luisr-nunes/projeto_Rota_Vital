package com.hemorede.estatistica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API das telas de Estatística: contrato, seleção de features e acesso só do hemocentro. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "HEMOCENTRO")
class EstatisticaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Lista variáveis reais e sintéticas; as reais não entram em correlação")
    void variaveis() throws Exception {
        mockMvc.perform(get("/api/estatistica/variaveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'diario.requisicoes')].n").value(hasItem(365)))
                .andExpect(jsonPath("$[?(@.id == 'real.estoquePorTipo')].n").value(hasItem(8)))
                .andExpect(jsonPath("$[?(@.fonte == 'real')].correlacionavel").value(not(hasItem(true))));
    }

    @Test
    @DisplayName("Descritiva do histórico diário: n, quartis em ordem e histograma por Sturges (10 classes)")
    void descritiva() throws Exception {
        mockMvc.perform(get("/api/estatistica/descritiva").param("variavel", "diario.requisicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variavel.n").value(365))
                .andExpect(jsonPath("$.histograma", hasSize(10)))
                .andExpect(jsonPath("$.primeirosValores", hasSize(12)))
                .andExpect(jsonPath("$.leituraAssimetria").isString());
    }

    @Test
    @DisplayName("Par x e y: as duas fórmulas de Pearson coincidem e a memória traz as 8 primeiras linhas")
    void par() throws Exception {
        mockMvc.perform(get("/api/estatistica/correlacao/par")
                        .param("x", "entregas.distancia").param("y", "entregas.tempo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.n").value(200))
                .andExpect(jsonPath("$.forca").value("forte"))
                .andExpect(jsonPath("$.direcao").value("positiva"))
                .andExpect(jsonPath("$.memoria", hasSize(8)))
                .andExpect(jsonPath("$.pontos", hasSize(200)));
    }

    @Test
    @DisplayName("Seleção de features para requisições/dia: acidentes e cirurgias entram, temperatura e dia do mês não")
    void selecaoDeFeatures() throws Exception {
        mockMvc.perform(get("/api/estatistica/previsao").param("alvo", "diario.requisicoes").param("x", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features[?(@.variavel.id == 'diario.acidentes')].selecionada").value(hasItem(true)))
                .andExpect(jsonPath("$.features[?(@.variavel.id == 'diario.cirurgias')].selecionada").value(hasItem(true)))
                .andExpect(jsonPath("$.features[?(@.variavel.id == 'diario.temperatura')].selecionada").value(hasItem(false)))
                .andExpect(jsonPath("$.features[?(@.variavel.id == 'diario.diaDoMes')].selecionada").value(hasItem(false)))
                .andExpect(jsonPath("$.yPrevisto").isNumber())
                .andExpect(jsonPath("$.r2").isNumber());
    }

    @Test
    @DisplayName("Matriz de correlação: diagonal 1 e erros claros para variável ou conjunto inexistente")
    void matrizEErros() throws Exception {
        mockMvc.perform(get("/api/estatistica/correlacao").param("conjunto", "entregas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.r[0][0]", closeTo(1.0, 1e-9)))
                .andExpect(jsonPath("$.variaveis", hasSize(4)));
        mockMvc.perform(get("/api/estatistica/descritiva").param("variavel", "diario.inexistente"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/estatistica/correlacao").param("conjunto", "outro"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/estatistica/correlacao/par").param("x", "real.estoquePorTipo").param("y", "diario.acidentes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DOADOR")
    @DisplayName("Doador não acessa a API de estatística (403)")
    void doadorSemAcesso() throws Exception {
        mockMvc.perform(get("/api/estatistica/variaveis")).andExpect(status().isForbidden());
    }
}
