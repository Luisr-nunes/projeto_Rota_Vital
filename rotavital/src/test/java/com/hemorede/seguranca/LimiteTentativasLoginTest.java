package com.hemorede.seguranca;

import com.hemorede.exception.MuitasTentativasException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Limite de tentativas de login")
class LimiteTentativasLoginTest {

    /** Relógio que o teste avança à mão. */
    private static final class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-11-21T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    @DisplayName("Bloqueia após o máximo de falhas e libera ao fim da janela")
    void bloqueiaELibera() {
        RelogioManual relogio = new RelogioManual();
        LimiteTentativasLogin limite = new LimiteTentativasLogin(3, Duration.ofMinutes(15), relogio);
        for (int i = 0; i < 3; i++) {
            limite.verificar("Ana@Teste.dev");
            limite.registrarFalha("ana@teste.dev");
        }
        MuitasTentativasException ex = assertThrows(MuitasTentativasException.class,
                () -> limite.verificar("ana@teste.dev"));
        assertTrue(ex.getMessage().contains("15 minuto(s)"));

        relogio.agora = relogio.agora.plus(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> limite.verificar("ana@teste.dev"));
    }

    @Test
    @DisplayName("Login certo zera as falhas; e-mails diferentes não se afetam")
    void sucessoZera() {
        LimiteTentativasLogin limite = new LimiteTentativasLogin(2, Duration.ofMinutes(15), new RelogioManual());
        limite.registrarFalha("ana@teste.dev");
        limite.registrarSucesso("ana@teste.dev");
        limite.registrarFalha("ana@teste.dev");
        assertDoesNotThrow(() -> limite.verificar("ana@teste.dev"));

        limite.registrarFalha("bia@teste.dev");
        limite.registrarFalha("bia@teste.dev");
        assertThrows(MuitasTentativasException.class, () -> limite.verificar("bia@teste.dev"));
        assertDoesNotThrow(() -> limite.verificar("ana@teste.dev"));
    }
}
