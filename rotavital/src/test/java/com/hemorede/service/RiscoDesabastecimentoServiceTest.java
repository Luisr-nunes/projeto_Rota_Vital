package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.DiasPrevisaoInvalidoException;
import com.hemorede.exception.ModeloProbabilisticoIndisponivelException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RiscoDesabastecimentoServiceTest {

    private final IndicadoresService service = new IndicadoresService();

    @Test
    void deveCalcularProbabilidadePoissonEOrdenarDoMaiorRisco() {
        Map<TipoSanguineo, Double> lambdas = lambdasComValor(0.1);
        lambdas.put(TipoSanguineo.A_POS, 1.0);
        lambdas.put(TipoSanguineo.O_NEG, 3.0);

        List<Bolsa> bolsas = List.of(
                bolsa(TipoSanguineo.A_POS, StatusBolsa.DISPONIVEL, 10),
                bolsa(TipoSanguineo.A_POS, StatusBolsa.RESERVADA, 10),
                bolsa(TipoSanguineo.A_POS, StatusBolsa.DISPONIVEL, -1)
        );

        var resposta = service.calcularRiscoDesabastecimento(1, bolsas, lambdas);

        assertEquals(8, resposta.riscos().size());
        assertEquals(TipoSanguineo.O_NEG, resposta.riscos().get(0).tipoSanguineo());

        var riscoAPos = resposta.riscos().stream()
                .filter(risco -> risco.tipoSanguineo() == TipoSanguineo.A_POS)
                .findFirst()
                .orElseThrow();

        assertEquals(1L, riscoAPos.estoqueAtual());
        assertEquals(1.0, riscoAPos.lambdaDiario());
        assertEquals(0.264241, riscoAPos.probabilidadeDesabastecimento());
    }

    @Test
    void deveUsarLambdaMultiplicadoPelaJanelaDeDias() {
        Map<TipoSanguineo, Double> lambdas = lambdasComValor(0.0);
        lambdas.put(TipoSanguineo.O_POS, 1.0);

        var resposta = service.calcularRiscoDesabastecimento(3, List.of(), lambdas);
        var riscoOPos = resposta.riscos().stream()
                .filter(risco -> risco.tipoSanguineo() == TipoSanguineo.O_POS)
                .findFirst()
                .orElseThrow();

        assertEquals(0.950213, riscoOPos.probabilidadeDesabastecimento());
    }

    @Test
    void deveRejeitarJanelaInvalida() {
        assertThrows(DiasPrevisaoInvalidoException.class,
                () -> service.calcularRiscoDesabastecimento(
                        0, List.of(), lambdasComValor(1.0)));
    }

    @Test
    void deveRecusarModeloSemLambdaParaTodosOsTipos() {
        Map<TipoSanguineo, Double> incompleto = new EnumMap<>(TipoSanguineo.class);
        incompleto.put(TipoSanguineo.O_POS, 1.0);

        assertThrows(ModeloProbabilisticoIndisponivelException.class,
                () -> service.calcularRiscoDesabastecimento(3, List.of(), incompleto));
    }

    private Map<TipoSanguineo, Double> lambdasComValor(double valor) {
        Map<TipoSanguineo, Double> lambdas = new EnumMap<>(TipoSanguineo.class);
        Arrays.stream(TipoSanguineo.values()).forEach(tipo -> lambdas.put(tipo, valor));
        return lambdas;
    }

    private Bolsa bolsa(TipoSanguineo tipo, StatusBolsa status, int diasAteValidade) {
        return Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(tipo)
                .dataColeta(LocalDate.now().minusDays(2))
                .dataValidade(LocalDate.now().plusDays(diasAteValidade))
                .status(status)
                .build();
    }
}
