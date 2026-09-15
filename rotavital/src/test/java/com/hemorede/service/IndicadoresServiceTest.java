package com.hemorede.service;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.indicadores.DadosIndicadoresSinteticos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Indicadores estatísticos")
class IndicadoresServiceTest {

    private final IndicadoresService indicadoresService = new IndicadoresService();

    @Test
    @DisplayName("Deve calcular os indicadores sobre a amostra sintética")
    void deveCalcularIndicadoresSinteticos() {
        var resposta = indicadoresService.calcular(
                5,
                DadosSinteticos.criarBolsasExemplo(),
                DadosIndicadoresSinteticos.criarAtendimentosExemplo()
        );

        assertEquals(12, resposta.estoque().totalDisponivel());
        assertEquals(3, resposta.estoque().porTipoSanguineo().get(TipoSanguineo.O_POS));
        assertEquals(1.5, resposta.estoque().mediaPorTipo());
        assertEquals(1.0, resposta.estoque().medianaPorTipo());
        assertEquals(1, resposta.estoque().minimoPorTipo());
        assertEquals(3, resposta.estoque().maximoPorTipo());
        assertEquals(0.71, resposta.estoque().desvioPadraoPorTipo());

        assertEquals(3, resposta.vencimento().quantidadeProximaDoVencimento());
        assertEquals(25.0, resposta.vencimento().percentualProximoDoVencimento());

        assertEquals(5, resposta.requisicoes().total());
        assertEquals(3, resposta.requisicoes().atendidas());
        assertEquals(65.0, resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
        assertEquals(2, resposta.requisicoes().porStatus().get(StatusRequisicao.APROVADA));
    }

    @Test
    @DisplayName("Deve retornar valores neutros quando não houver dados")
    void deveTratarAmostraVazia() {
        var resposta = indicadoresService.calcular(5, List.of(), List.of());

        assertEquals(0, resposta.estoque().totalDisponivel());
        assertEquals(0.0, resposta.vencimento().percentualProximoDoVencimento());
        assertEquals(0, resposta.requisicoes().atendidas());
        assertNull(resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
    }

    @Test
    @DisplayName("Deve rejeitar janela de vencimento negativa")
    void deveRejeitarJanelaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> indicadoresService.calcular(-1));
    }
}
