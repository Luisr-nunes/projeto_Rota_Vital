package com.hemorede.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.CalcularRotaRequest;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do POST /api/rotas/calcular (HU05 - planejar rota
 * de entrega), sobre o grafo sintético do Hemocentro e os 5 hospitais.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RotaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private RequisicaoRepository requisicaoRepository;

    @Test
    @DisplayName("Calcula a rota mínima do Hemocentro (N0) até o hospital com nó N5 (custo 10.0)")
    void deveCalcularRotaMinimaParaHospitalComNoValido() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setNome("Hospital Central II");
        hospital.setCnpj("55666777000188");
        hospital.setCodigoNo("N5");
        hospital = hospitalRepository.save(hospital);

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.URGENTE);
        requisicao = requisicaoRepository.save(requisicao);

        mockMvc.perform(post("/api/rotas/calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularRotaRequest(requisicao.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanciaTotalKm").value(10.0))
                .andExpect(jsonPath("$.caminho[0]").value("N0"))
                .andExpect(jsonPath("$.caminho[1]").value("N5"));
    }

    @Test
    @DisplayName("Retorna 422 quando o hospital solicitante não tem nó associado no grafo")
    void deveRetornar422ParaHospitalSemNo() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setNome("Hospital Sem Nó no Grafo");
        hospital.setCnpj("66777888000199");
        hospital = hospitalRepository.save(hospital);

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.NORMAL);
        requisicao = requisicaoRepository.save(requisicao);

        mockMvc.perform(post("/api/rotas/calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularRotaRequest(requisicao.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("RotaIndisponivelException"));
    }

    @Test
    @DisplayName("Retorna 404 quando a requisição informada não existe")
    void deveRetornar404ParaRequisicaoInexistente() throws Exception {
        mockMvc.perform(post("/api/rotas/calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CalcularRotaRequest(999_999L))))
                .andExpect(status().isNotFound());
    }
}
