package com.hemorede.domain.regras;

import com.hemorede.domain.enums.Sexo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RN-04 - Elegibilidade do doador")
class RegraElegibilidadeDoadorTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 20);
    private static final LocalDate ADULTO = HOJE.minusYears(30);

    @Test
    void doadorSemHistoricoEApto() {
        var r = RegraElegibilidadeDoador.avaliar(true, Sexo.FEMININO, ADULTO, 60.0, null, 0, HOJE);
        assertTrue(r.apto());
        assertTrue(r.motivos().isEmpty());
        assertEquals(HOJE, r.proximaDataPossivel());
    }

    @Test
    void intervaloDependeDoSexo() {
        LocalDate ultima = HOJE.minusDays(70);
        assertTrue(RegraElegibilidadeDoador.avaliar(true, Sexo.MASCULINO, ADULTO, 70.0, ultima, 1, HOJE).apto());
        var mulher = RegraElegibilidadeDoador.avaliar(true, Sexo.FEMININO, ADULTO, 70.0, ultima, 1, HOJE);
        assertFalse(mulher.apto());
        assertEquals(ultima.plusDays(90), mulher.proximaDataPossivel());
        assertTrue(mulher.motivos().get(0).contains("faltam 20"));
    }

    @Test
    void sexoNaoInformadoUsaIntervaloLegadoDe60Dias() {
        assertEquals(60, RegraElegibilidadeDoador.intervaloMinimoDias(null));
        assertTrue(RegraElegibilidadeDoador.avaliar(true, null, null, null, HOJE.minusDays(60), 0, HOJE).apto());
        assertFalse(RegraElegibilidadeDoador.avaliar(true, null, null, null, HOJE.minusDays(59), 0, HOJE).apto());
    }

    @Test
    void idadePesoInatividadeEFrequencia() {
        assertFalse(RegraElegibilidadeDoador.avaliar(true, Sexo.MASCULINO, HOJE.minusYears(15), 70.0, null, 0, HOJE).apto());
        assertFalse(RegraElegibilidadeDoador.avaliar(true, Sexo.MASCULINO, HOJE.minusYears(70), 70.0, null, 0, HOJE).apto());
        assertTrue(RegraElegibilidadeDoador.avaliar(true, Sexo.MASCULINO, HOJE.minusYears(69), 70.0, null, 0, HOJE).apto());
        assertFalse(RegraElegibilidadeDoador.avaliar(true, Sexo.MASCULINO, ADULTO, 49.9, null, 0, HOJE).apto());
        assertFalse(RegraElegibilidadeDoador.avaliar(false, Sexo.MASCULINO, ADULTO, 70.0, null, 0, HOJE).apto());
        assertFalse(RegraElegibilidadeDoador.avaliar(true, Sexo.FEMININO, ADULTO, 70.0, null, 3, HOJE).apto());
        assertTrue(RegraElegibilidadeDoador.avaliar(true, Sexo.MASCULINO, ADULTO, 70.0, null, 3, HOJE).apto());
    }

    @Test
    void acumulaTodosOsMotivos() {
        var r = RegraElegibilidadeDoador.avaliar(false, Sexo.FEMININO, HOJE.minusYears(15), 40.0, HOJE.minusDays(1), 3, HOJE);
        assertEquals(5, r.motivos().size());
    }
}
