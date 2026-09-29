package com.hemorede.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.repository.BolsaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ValidadeService - expurgo de bolsas vencidas")
class ValidadeServiceTest {

    @Test
    @DisplayName("Bolsas vencidas disponíveis são marcadas como DESCARTADA e persistidas")
    void descartaBolsasVencidas() {
        BolsaRepository repository = mock(BolsaRepository.class);
        Bolsa b1 = Bolsa.builder().id(1L).dataValidade(LocalDate.now().minusDays(1)).build();
        Bolsa b2 = Bolsa.builder().id(2L).dataValidade(LocalDate.now().minusDays(30)).build();
        List<Bolsa> vencidas = new ArrayList<>(List.of(b1, b2));
        when(repository.findByStatusAndDataValidadeBefore(StatusBolsa.DISPONIVEL, LocalDate.now()))
                .thenReturn(vencidas);

        new ValidadeService(repository).expurgarBolsasVencidas();

        assertEquals(StatusBolsa.DESCARTADA, b1.getStatus());
        assertEquals(StatusBolsa.DESCARTADA, b2.getStatus());
        verify(repository).saveAll(vencidas);
    }

    @Test
    @DisplayName("Sem bolsas vencidas: nada é alterado e não ocorre exceção")
    void semBolsasVencidas() {
        BolsaRepository repository = mock(BolsaRepository.class);
        when(repository.findByStatusAndDataValidadeBefore(StatusBolsa.DISPONIVEL, LocalDate.now()))
                .thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> new ValidadeService(repository).expurgarBolsasVencidas());

        verify(repository).saveAll(new ArrayList<>());
    }
}
