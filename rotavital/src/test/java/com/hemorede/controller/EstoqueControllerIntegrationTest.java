package com.hemorede.controller;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.repository.BolsaRepository;

/**
 * Testes de integração de {@code GET /api/estoques/bolsas} (HU02 - Consultar
 * estoque), passando pela camada HTTP (MockMvc) com o contexto Spring real e
 * o banco H2, cobrindo os cenários de sucesso, sem resultado e parâmetro
 * inválido.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EstoqueControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BolsaRepository bolsaRepository;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        bolsaRepository.deleteAll();
    }

    private Bolsa novaBolsa(TipoSanguineo tipo, HemoComponente componente, StatusBolsa status,
                             LocalDate dataColeta, LocalDate dataValidade) {
        return Bolsa.builder()
                .tipoSanguineo(tipo)
                .hemoComponente(componente)
                .status(status)
                .dataColeta(dataColeta)
                .dataValidade(dataValidade)
                .build();
    }

    @Test
    @DisplayName("GET /api/estoques/bolsas?tipo=O_NEG&componente=HEMACIAS retorna só DISPONIVEL, não vencidas, "
            + "ordenadas da validade mais próxima para a mais distante")
    void deveListarBolsasDisponiveisOrdenadasPorValidade() throws Exception {
        LocalDate hoje = LocalDate.now();

        // Bolsa compatível, mas de outro tipo sanguíneo: não deve aparecer no resultado.
        bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, HemoComponente.HEMACIAS,
                StatusBolsa.DISPONIVEL, hoje.minusDays(5), hoje.plusDays(1)));

        // Mesmo tipo/componente, mas RESERVADA: não deve aparecer.
        bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS,
                StatusBolsa.RESERVADA, hoje.minusDays(5), hoje.plusDays(3)));

        // Mesmo tipo/componente, mas vencida: não deve aparecer.
        bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS,
                StatusBolsa.DISPONIVEL, hoje.minusDays(50), hoje.minusDays(1)));

        // Três bolsas elegíveis, em ordem de inserção propositalmente embaralhada.
        Bolsa venceEm20 = bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS,
                StatusBolsa.DISPONIVEL, hoje.minusDays(1), hoje.plusDays(20)));
        Bolsa venceEm2 = bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS,
                StatusBolsa.DISPONIVEL, hoje.minusDays(10), hoje.plusDays(2)));
        Bolsa venceEm8 = bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS,
                StatusBolsa.DISPONIVEL, hoje.minusDays(5), hoje.plusDays(8)));

        mockMvc.perform(get("/api/estoques/bolsas")
                        .param("tipo", "O_NEG")
                        .param("componente", "HEMACIAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(3)))
                .andExpect(jsonPath("$.mensagem").doesNotExist())
                .andExpect(jsonPath("$.bolsas[0].id").value(venceEm2.getId()))
                .andExpect(jsonPath("$.bolsas[1].id").value(venceEm8.getId()))
                .andExpect(jsonPath("$.bolsas[2].id").value(venceEm20.getId()))
                .andExpect(jsonPath("$.bolsas[0].tipoSanguineo").value("O_NEG"))
                .andExpect(jsonPath("$.bolsas[0].hemoComponente").value("HEMACIAS"));
    }

    @Test
    @DisplayName("Cenário 2 da HU02: filtro sem bolsas retorna 200, lista vazia e mensagem 'nenhuma bolsa encontrada'")
    void deveRetornarListaVaziaComMensagemQuandoNaoHouverBolsas() throws Exception {
        mockMvc.perform(get("/api/estoques/bolsas")
                        .param("tipo", "AB_NEG")
                        .param("componente", "PLASMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(0)))
                .andExpect(jsonPath("$.mensagem").value("nenhuma bolsa encontrada"));
    }

    @Test
    @DisplayName("Tipo sanguíneo inválido retorna 400")
    void deveRetornar400ParaTipoSanguineoInvalido() throws Exception {
        mockMvc.perform(get("/api/estoques/bolsas")
                        .param("tipo", "TIPO_QUE_NAO_EXISTE")
                        .param("componente", "HEMACIAS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Hemocomponente inválido retorna 400")
    void deveRetornar400ParaHemoComponenteInvalido() throws Exception {
        mockMvc.perform(get("/api/estoques/bolsas")
                        .param("tipo", "O_NEG")
                        .param("componente", "COMPONENTE_QUE_NAO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Parâmetro obrigatório ausente retorna 400")
    void deveRetornar400ParaParametroAusente() throws Exception {
        mockMvc.perform(get("/api/estoques/bolsas")
                        .param("tipo", "O_NEG"))
                .andExpect(status().isBadRequest());
    }
}
