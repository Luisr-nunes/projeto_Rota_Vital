package com.hemorede.controller;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integração: Endpoint GET /api/indicadores (HU07)")
class IndicadoresControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BolsaRepository bolsaRepository;

    @Autowired
    private RequisicaoRepository requisicaoRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        requisicaoRepository.deleteAll();
        bolsaRepository.deleteAll();
    }

    @Test
    @DisplayName("Cenário 2 da HU07: Banco vazio deve retornar estado neutro sem números inventados")
    void deveRetornarEstadoVazioQuandoBancoEstiverVazio() throws Exception {
        mockMvc.perform(get("/api/indicadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estoque.totalDisponivel", is(0)))
                .andExpect(jsonPath("$.estoque.mediaPorTipo", is(0.0)))
                .andExpect(jsonPath("$.vencimento.quantidadeProximaDoVencimento", is(0)))
                .andExpect(jsonPath("$.vencimento.percentualProximoDoVencimento", is(0.0)))
                .andExpect(jsonPath("$.requisicoes.total", is(0)))
                .andExpect(jsonPath("$.requisicoes.atendidas", is(0)))
                .andExpect(jsonPath("$.requisicoes.tempoMedioAtendimentoEmMinutos", nullValue()));
    }

    @Test
    @DisplayName("Cenário 1 da HU07: Deve refletir dados inseridos no banco via repositório")
    void deveRefletirDadosInseridosNoBanco() throws Exception {
        requisicaoRepository.deleteAll();
        bolsaRepository.deleteAll();

        // 1. Obtém ou cria hospital para atender a Foreign Key da requisição
        Hospital hospital = hospitalRepository.findAll().stream().findFirst().orElseGet(() ->
                hospitalRepository.save(new Hospital(
                        null, "Hospital de Teste", "99.999.999/0001-99", "Rua Teste, 100", -8.05, -34.88, "N99", null, null
                ))
        );

        // 2. Insere bolsa no banco
        Bolsa bolsa = Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.O_POS)
                .dataColeta(LocalDate.now().minusDays(2))
                .dataValidade(LocalDate.now().plusDays(10))
                .status(StatusBolsa.DISPONIVEL)
                .build();
        bolsaRepository.save(bolsa);

        // 3. Insere requisição pendente no banco
        Requisicao req = new Requisicao();
        req.setHospitalSolicitante(hospital);
        req.setStatus(StatusRequisicao.PENDENTE);
        req.setPrioridade(Prioridade.URGENTE);
        req.setDataSolicitacao(LocalDateTime.now());
        requisicaoRepository.save(req);

        // 4. Consulta o endpoint e valida que os dados batem 100% com o banco
        mockMvc.perform(get("/api/indicadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estoque.totalDisponivel", is(1)))
                .andExpect(jsonPath("$.estoque.porTipoSanguineo.O_POS", is(1)))
                .andExpect(jsonPath("$.requisicoes.total", is(1)))
                .andExpect(jsonPath("$.requisicoes.atendidas", is(0)))
                .andExpect(jsonPath("$.requisicoes.porStatus.PENDENTE", is(1)));
    }
}
