package com.hemorede.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.repository.BolsaRepository;

/**
 * Testes de integração de {@code GET /api/v1/estoques/bolsas} (HU02, PI2-182,
 * issue #58): caminho versionado e filtro opcional de validade
 * {@code venceEmDias} (bolsas que vencem em até N dias, contando hoje).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EstoqueBolsasValidadeIntegrationTest {

    private static final String URL_V1 = "/api/v1/estoques/bolsas";
    private static final String URL_LEGADA = "/api/estoques/bolsas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BolsaRepository bolsaRepository;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        bolsaRepository.deleteAll();
    }

    private Bolsa salvar(TipoSanguineo tipo, HemoComponente componente, StatusBolsa status, LocalDate validade) {
        return bolsaRepository.save(Bolsa.builder()
                .tipoSanguineo(tipo)
                .hemoComponente(componente)
                .status(status)
                .dataColeta(LocalDate.now().minusDays(30))
                .dataValidade(validade)
                .build());
    }

    @Test
    @DisplayName("Sem filtro de validade, o caminho /api/v1 lista todas as bolsas elegíveis em ordem FEFO")
    void v1SemFiltroListaTodasEmOrdemFefo() throws Exception {
        LocalDate hoje = LocalDate.now();
        Bolsa venceEm20 = salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, hoje.plusDays(20));
        Bolsa venceEm2 = salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, hoje.plusDays(2));

        mockMvc.perform(get(URL_V1).param("tipo", "O_NEG").param("componente", "HEMACIAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(2)))
                .andExpect(jsonPath("$.mensagem").doesNotExist())
                .andExpect(jsonPath("$.bolsas[0].id").value(venceEm2.getId()))
                .andExpect(jsonPath("$.bolsas[1].id").value(venceEm20.getId()));
    }

    @Test
    @DisplayName("venceEmDias=10 devolve só as bolsas que vencem em até 10 dias, ainda em ordem FEFO")
    void filtraPorVenceEmDias() throws Exception {
        LocalDate hoje = LocalDate.now();
        Bolsa venceEm20 = salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, hoje.plusDays(20));
        Bolsa venceEm8 = salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, hoje.plusDays(8));
        Bolsa venceEm2 = salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, hoje.plusDays(2));
        // Dentro do prazo, mas não elegíveis: vencida e reservada.
        salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, hoje.minusDays(1));
        salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.RESERVADA, hoje.plusDays(3));

        mockMvc.perform(get(URL_V1)
                        .param("tipo", "O_NEG")
                        .param("componente", "HEMACIAS")
                        .param("venceEmDias", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(2)))
                .andExpect(jsonPath("$.bolsas[0].id").value(venceEm2.getId()))
                .andExpect(jsonPath("$.bolsas[1].id").value(venceEm8.getId()))
                .andExpect(jsonPath("$.mensagem").doesNotExist());

        // venceEm20 só entra quando a janela cobre 20 dias (limite inclusivo).
        mockMvc.perform(get(URL_V1)
                        .param("tipo", "O_NEG")
                        .param("componente", "HEMACIAS")
                        .param("venceEmDias", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(3)))
                .andExpect(jsonPath("$.bolsas[2].id").value(venceEm20.getId()));
    }

    @Test
    @DisplayName("venceEmDias=0 devolve só as bolsas que vencem hoje")
    void venceEmDiasZeroDevolveSoAsQueVencemHoje() throws Exception {
        LocalDate hoje = LocalDate.now();
        Bolsa venceHoje = salvar(TipoSanguineo.A_POS, HemoComponente.PLAQUETAS, StatusBolsa.DISPONIVEL, hoje);
        salvar(TipoSanguineo.A_POS, HemoComponente.PLAQUETAS, StatusBolsa.DISPONIVEL, hoje.plusDays(1));

        mockMvc.perform(get(URL_V1)
                        .param("tipo", "A_POS")
                        .param("componente", "PLAQUETAS")
                        .param("venceEmDias", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(1)))
                .andExpect(jsonPath("$.bolsas[0].id").value(venceHoje.getId()));
    }

    @Test
    @DisplayName("Filtro de validade sem nenhuma bolsa na janela retorna 200, lista vazia e mensagem")
    void filtroSemResultadoRetornaListaVaziaComMensagem() throws Exception {
        salvar(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL, LocalDate.now().plusDays(30));

        mockMvc.perform(get(URL_V1)
                        .param("tipo", "O_NEG")
                        .param("componente", "HEMACIAS")
                        .param("venceEmDias", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(0)))
                .andExpect(jsonPath("$.mensagem").value("nenhuma bolsa encontrada"));
    }

    @Test
    @DisplayName("venceEmDias negativo retorna 400")
    void venceEmDiasNegativoRetorna400() throws Exception {
        mockMvc.perform(get(URL_V1)
                        .param("tipo", "O_NEG")
                        .param("componente", "HEMACIAS")
                        .param("venceEmDias", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("venceEmDias não numérico retorna 400")
    void venceEmDiasNaoNumericoRetorna400() throws Exception {
        mockMvc.perform(get(URL_V1)
                        .param("tipo", "O_NEG")
                        .param("componente", "HEMACIAS")
                        .param("venceEmDias", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tipo e componente continuam obrigatórios no caminho /api/v1")
    void tipoEComponenteContinuamObrigatoriosNoV1() throws Exception {
        mockMvc.perform(get(URL_V1).param("tipo", "O_NEG"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_V1).param("componente", "HEMACIAS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("O caminho legado /api/estoques/bolsas continua respondendo, inclusive com o filtro")
    void caminhoLegadoContinuaFuncionandoComFiltro() throws Exception {
        LocalDate hoje = LocalDate.now();
        Bolsa venceEm3 = salvar(TipoSanguineo.B_POS, HemoComponente.PLASMA, StatusBolsa.DISPONIVEL, hoje.plusDays(3));
        salvar(TipoSanguineo.B_POS, HemoComponente.PLASMA, StatusBolsa.DISPONIVEL, hoje.plusDays(15));

        mockMvc.perform(get(URL_LEGADA)
                        .param("tipo", "B_POS")
                        .param("componente", "PLASMA")
                        .param("venceEmDias", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bolsas", hasSize(1)))
                .andExpect(jsonPath("$.bolsas[0].id").value(venceEm3.getId()));
    }

    @Test
    @DisplayName("Os demais endpoints de estoque também respondem sob /api/v1/estoques")
    void listagemDeEstoquesRespondeNoV1() throws Exception {
        mockMvc.perform(get("/api/v1/estoques"))
                .andExpect(status().isOk());
    }
}
