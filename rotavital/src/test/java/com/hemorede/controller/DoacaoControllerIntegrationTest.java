package com.hemorede.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Doador;
import com.hemorede.dto.DoacaoRequest;
import com.hemorede.repository.DoadorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DoacaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DoadorRepository doadorRepository;

    @Test
    @DisplayName("POST /api/doacoes registra doação e retorna a bolsa disponível")
    void deveRegistrarDoacao() throws Exception {
        Doador doador = criarDoador("100.200.300-01", LocalDate.now().minusDays(90));
        DoacaoRequest request = novaRequisicao(doador.getId());

        mockMvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/doacoes/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoSanguineo").value("O_POS"))
                .andExpect(jsonPath("$.hemoComponente").value("HEMACIAS"))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.doador.id").value(doador.getId()));
    }

    @Test
    @DisplayName("POST /api/doacoes retorna 422 quando o intervalo mínimo não foi cumprido")
    void deveRejeitarDoacaoForaDoIntervalo() throws Exception {
        Doador doador = criarDoador("100.200.300-02", LocalDate.now().minusDays(10));

        mockMvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novaRequisicao(doador.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("DoacaoForaDoIntervaloException"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/doacoes retorna 422 quando o doador está inativo")
    void deveRejeitarDoadorInativo() throws Exception {
        Doador doador = criarDoador("100.200.300-03", LocalDate.now().minusDays(90));
        doador.setAtivo(false);
        doadorRepository.save(doador);

        mockMvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novaRequisicao(doador.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("DoadorInativoException"))
                .andExpect(jsonPath("$.mensagem").value("Doador " + doador.getId()
                        + " está inativo e não pode realizar doações."));
    }

    @Test
    @DisplayName("POST /api/doacoes retorna 400 quando falta campo obrigatório")
    void deveRejeitarCampoObrigatorioAusente() throws Exception {
        mockMvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private Doador criarDoador(String cpf, LocalDate dataUltimaDoacao) {
        Doador doador = new Doador(
                null,
                "Doador de Teste",
                cpf,
                TipoSanguineo.O_POS,
                dataUltimaDoacao,
                new ArrayList<>()
        );
        return doadorRepository.save(doador);
    }

    private DoacaoRequest novaRequisicao(Long doadorId) {
        return new DoacaoRequest(
                doadorId,
                TipoSanguineo.O_POS,
                HemoComponente.HEMACIAS,
                LocalDate.now(),
                LocalDate.now().plusDays(35)
        );
    }
}
