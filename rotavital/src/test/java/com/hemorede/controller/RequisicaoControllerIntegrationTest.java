package com.hemorede.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Hospital;
import com.hemorede.dto.ItemRequisicaoRequest;
import com.hemorede.dto.RequisicaoRequest;
import com.hemorede.repository.HospitalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cenários BDD da HU03 — Registrar requisição hospitalar (PI2-130).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RequisicaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Test
    @DisplayName("Dado hospital existente, quando registrar requisição válida, então responde 201 com status PENDENTE")
    void deveRegistrarRequisicaoValida() throws Exception {
        Hospital hospital = criarHospital("77888999000101");
        RequisicaoRequest request = new RequisicaoRequest(hospital.getId(), Prioridade.URGENTE,
                List.of(new ItemRequisicaoRequest(HemoComponente.HEMACIAS, TipoSanguineo.O_NEG, 2)));

        mockMvc.perform(post("/api/requisicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/requisicoes/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.prioridade").value("URGENTE"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].tipoSanguineo").value("O_NEG"));
    }

    @Test
    @DisplayName("Quando a quantidade for menor ou igual a zero, então não salva e responde 400")
    void deveRejeitarQuantidadeInvalida() throws Exception {
        Hospital hospital = criarHospital("77888999000102");
        RequisicaoRequest request = new RequisicaoRequest(hospital.getId(), Prioridade.NORMAL,
                List.of(new ItemRequisicaoRequest(HemoComponente.HEMACIAS, TipoSanguineo.A_POS, 0)));

        mockMvc.perform(post("/api/requisicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Quando a requisição vier sem itens, então responde 400")
    void deveRejeitarRequisicaoSemItens() throws Exception {
        Hospital hospital = criarHospital("77888999000103");
        RequisicaoRequest request = new RequisicaoRequest(hospital.getId(), Prioridade.NORMAL, List.of());

        mockMvc.perform(post("/api/requisicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Quando o hospital não existir, então responde 404")
    void deveRejeitarHospitalInexistente() throws Exception {
        RequisicaoRequest request = new RequisicaoRequest(999_999L, Prioridade.NORMAL,
                List.of(new ItemRequisicaoRequest(HemoComponente.PLASMA, TipoSanguineo.AB_POS, 1)));

        mockMvc.perform(post("/api/requisicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Hospital não encontrado: 999999"));
    }

    private Hospital criarHospital(String cnpj) {
        Hospital hospital = new Hospital();
        hospital.setNome("Hospital Teste HU03 " + cnpj);
        hospital.setCnpj(cnpj);
        hospital.setEndereco("Rua de Teste, 100");
        hospital.setLatitude(-8.05);
        hospital.setLongitude(-34.9);
        return hospitalRepository.save(hospital);
    }
}
