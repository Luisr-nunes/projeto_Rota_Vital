package com.hemorede.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemorede.domain.model.Hospital;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração do CRUD de Hospital, passando pela camada HTTP
 * (MockMvc) com o contexto Spring real e o banco H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HospitalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/hospitais cria e GET busca o hospital criado")
    void deveCriarEBuscarHospital() throws Exception {
        Hospital novo = new Hospital();
        novo.setNome("Hospital Municipal Central");
        novo.setCnpj("11222333000144");
        novo.setEndereco("Av. Central, 100");
        novo.setCodigoNo("N1");

        String resposta = mockMvc.perform(post("/api/hospitais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Hospital Municipal Central"))
                .andReturn().getResponse().getContentAsString();

        Hospital criado = objectMapper.readValue(resposta, Hospital.class);

        mockMvc.perform(get("/api/hospitais/{id}", criado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("11222333000144"));
    }

    @Test
    @DisplayName("GET /api/hospitais/{id} com id inexistente retorna 404")
    void deveRetornar404ParaHospitalInexistente() throws Exception {
        mockMvc.perform(get("/api/hospitais/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/hospitais lista os hospitais cadastrados")
    void deveListarHospitais() throws Exception {
        Hospital novo = new Hospital();
        novo.setNome("Hospital para Listagem");
        novo.setCnpj("99888777000166");

        mockMvc.perform(post("/api/hospitais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novo)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hospitais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));
    }
}
