package com.hemorede.domain.regras;

import com.hemorede.domain.regras.RegraProgressoEntrega.Ponto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RN-10 - Posição estimada da entrega no trajeto")
class RegraProgressoEntregaTest {

    private static final List<Ponto> TRAJETO = List.of(new Ponto(0, 0), new Ponto(0, 10), new Ponto(10, 10));
    private static final List<Double> PESOS = List.of(10.0, 30.0);

    @Test
    void fracaoDoTempoDecorrido() {
        assertEquals(0.5, RegraProgressoEntrega.fracaoDecorrida(30, 60));
        assertEquals(0.0, RegraProgressoEntrega.fracaoDecorrida(-5, 60));
        assertEquals(1.0, RegraProgressoEntrega.fracaoDecorrida(90, 60));
        assertEquals(1.0, RegraProgressoEntrega.fracaoDecorrida(0, 0));
    }

    @Test
    void inicioMeioEFimDoTrajeto() {
        var inicio = RegraProgressoEntrega.interpolar(TRAJETO, PESOS, 0);
        assertEquals(new Ponto(0, 0), inicio.ponto());
        assertEquals(0, inicio.trechoAtual());

        // 25% de 40 = 10: exatamente no fim do primeiro trecho
        var quarto = RegraProgressoEntrega.interpolar(TRAJETO, PESOS, 0.25);
        assertEquals(0, quarto.trechoAtual());
        assertEquals(10.0, quarto.ponto().lng(), 1e-9);

        // 62,5% de 40 = 25: 15 de 30 no segundo trecho (metade)
        var meio = RegraProgressoEntrega.interpolar(TRAJETO, PESOS, 0.625);
        assertEquals(1, meio.trechoAtual());
        assertEquals(5.0, meio.ponto().lat(), 1e-9);
        assertEquals(62.5, meio.percentual());

        var fim = RegraProgressoEntrega.interpolar(TRAJETO, PESOS, 1.0);
        assertEquals(new Ponto(10, 10), fim.ponto());
        assertEquals(100.0, fim.percentual());
    }

    @Test
    void trajetoDeUmPontoJaChegou() {
        assertEquals(100.0, RegraProgressoEntrega.interpolar(List.of(new Ponto(1, 1)), List.of(), 0.1).percentual());
    }

    @Test
    void entradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> RegraProgressoEntrega.interpolar(List.of(), List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> RegraProgressoEntrega.interpolar(TRAJETO, List.of(1.0), 0));
    }
}
