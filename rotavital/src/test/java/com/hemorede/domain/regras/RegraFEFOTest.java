package com.hemorede.domain.regras;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.hemorede.domain.enums.TipoSanguineo.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RN-02 - FEFO com desempate por tipo idêntico e coleta")
class RegraFEFOTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 20);

    @Test
    void validadeMaisProximaSaiPrimeiro() {
        assertTrue(RegraFEFO.comparar(D, O_NEG, D, D.plusDays(1), A_POS, D, A_POS) < 0);
        assertTrue(RegraFEFO.comparar(D.plusDays(3), A_POS, D, D, O_NEG, D, A_POS) > 0);
    }

    @Test
    void mesmaValidadePrefereTipoIdenticoPreservandoUniversal() {
        // pedido A+: A+ e O- vencem no mesmo dia; sai A+ e o O- fica para quem só aceita O-
        assertTrue(RegraFEFO.comparar(D, A_POS, D, D, O_NEG, D, A_POS) < 0);
        assertTrue(RegraFEFO.comparar(D, O_NEG, D, D, A_POS, D, A_POS) > 0);
    }

    @Test
    void mesmaValidadeEMesmoTipoPrefereColetaMaisAntiga() {
        assertTrue(RegraFEFO.comparar(D, A_POS, D.minusDays(5), D, A_POS, D.minusDays(1), A_POS) < 0);
        assertTrue(RegraFEFO.comparar(D, A_POS, null, D, A_POS, D, A_POS) > 0);
        assertEquals(0, RegraFEFO.comparar(D, A_POS, null, D, A_POS, null, A_POS));
    }

    @Test
    void auditoriaDeSaidaDetectaViolacao() {
        assertTrue(RegraFEFO.respeitaFEFO(List.of(D, D.plusDays(1)), List.of(D.plusDays(1), D.plusDays(9))));
        assertFalse(RegraFEFO.respeitaFEFO(List.of(D.plusDays(10)), List.of(D.plusDays(2))));
        assertTrue(RegraFEFO.respeitaFEFO(List.of(), List.of(D)));
        assertTrue(RegraFEFO.respeitaFEFO(List.of(D), List.of()));
    }
}
