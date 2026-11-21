package com.hemorede.domain.regras;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.hemorede.domain.enums.HemoComponente.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RN-03 - Validade da bolsa")
class RegraValidadeBolsaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 20);

    @Test
    void venceNoFimDoDiaDaValidade() {
        assertFalse(RegraValidadeBolsa.vencida(HOJE, HOJE));
        assertTrue(RegraValidadeBolsa.vencida(HOJE.minusDays(1), HOJE));
        assertTrue(RegraValidadeBolsa.vencida(null, HOJE));
    }

    @Test
    void validadePadraoPorComponente() {
        assertEquals(HOJE.plusDays(42), RegraValidadeBolsa.validadePadrao(HEMACIAS, HOJE));
        assertEquals(HOJE.plusDays(5), RegraValidadeBolsa.validadePadrao(PLAQUETAS, HOJE));
        assertEquals(HOJE.plusDays(365), RegraValidadeBolsa.validadePadrao(PLASMA, HOJE));
    }

    @Test
    void diasParaVencer() {
        assertEquals(0, RegraValidadeBolsa.diasParaVencer(HOJE, HOJE));
        assertEquals(3, RegraValidadeBolsa.diasParaVencer(HOJE.plusDays(3), HOJE));
        assertEquals(-2, RegraValidadeBolsa.diasParaVencer(HOJE.minusDays(2), HOJE));
    }

    @Test
    void datasValidasNaoGeramProblemas() {
        assertTrue(RegraValidadeBolsa.validarDatas(HEMACIAS, HOJE, HOJE.plusDays(42), HOJE).isEmpty());
    }

    @Test
    void detectaColetaFuturaValidadeAnteriorEPrazoExcedido() {
        assertEquals(1, RegraValidadeBolsa.validarDatas(HEMACIAS, HOJE.plusDays(1), HOJE.plusDays(10), HOJE).size());
        assertFalse(RegraValidadeBolsa.validarDatas(HEMACIAS, HOJE, HOJE, HOJE).isEmpty());
        assertFalse(RegraValidadeBolsa.validarDatas(PLAQUETAS, HOJE, HOJE.plusDays(6), HOJE).isEmpty());
        assertFalse(RegraValidadeBolsa.validarDatas(null, HOJE, HOJE.plusDays(1), HOJE).isEmpty());
    }
}
