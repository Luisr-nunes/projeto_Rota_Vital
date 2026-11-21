package com.hemorede.domain.regras;

import com.hemorede.domain.enums.TipoRefrigeracao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.hemorede.domain.enums.HemoComponente.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RN-06 - Cadeia fria no transporte")
class RegraRefrigeracaoTest {

    @Test
    void veiculoCompativelSoComAMesmaFaixa() {
        assertTrue(RegraRefrigeracao.veiculoCompativel(TipoRefrigeracao.REFRIGERADO_2_6, HEMACIAS));
        assertFalse(RegraRefrigeracao.veiculoCompativel(TipoRefrigeracao.REFRIGERADO_2_6, PLASMA));
        assertFalse(RegraRefrigeracao.veiculoCompativel(null, PLASMA));
    }

    @Test
    void plasmaECrioCabemNoMesmoVeiculoHemaciasEPlasmaNao() {
        assertTrue(RegraRefrigeracao.despachoUnicoPossivel(List.of(PLASMA, CRIOPRECIPITADO)));
        assertFalse(RegraRefrigeracao.despachoUnicoPossivel(List.of(HEMACIAS, PLASMA)));
        assertEquals(3, RegraRefrigeracao.exigidas(List.of(HEMACIAS, PLASMA, PLAQUETAS)).size());
        assertFalse(RegraRefrigeracao.despachoUnicoPossivel(List.of()));
    }
}
