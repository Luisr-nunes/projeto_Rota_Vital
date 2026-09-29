package com.hemorede.service;

import com.hemorede.domain.model.Doador;
import com.hemorede.exception.DoacaoForaDoIntervaloException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DoacaoService - regra de intervalo mínimo entre doações")
class DoacaoServiceTest {

    private static final int INTERVALO = 60;

    private DoadorRepository repository;
    private DoacaoService service;

    @BeforeEach
    void setUp() {
        repository = mock(DoadorRepository.class);
        service = new DoacaoService(repository, mock(BolsaRepository.class));
        ReflectionTestUtils.setField(service, "intervaloMinimoDias", INTERVALO);
    }

    private Doador doadorComUltimaDoacao(LocalDate ultima) {
        Doador doador = new Doador();
        doador.setId(1L);
        doador.setDataUltimaDoacao(ultima);
        when(repository.findById(1L)).thenReturn(Optional.of(doador));
        return doador;
    }

    @Test
    @DisplayName("Doador inexistente: lança IllegalArgumentException e não salva")
    void doadorInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.registrarDoacao(99L, LocalDate.now()));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Primeira doação (sem data anterior): registra e salva")
    void primeiraDoacao() {
        Doador doador = doadorComUltimaDoacao(null);
        LocalDate hoje = LocalDate.now();

        service.registrarDoacao(1L, hoje);

        assertEquals(hoje, doador.getDataUltimaDoacao());
        verify(repository).save(doador);
    }

    @Test
    @DisplayName("Exatamente no intervalo mínimo (60 dias): permitido")
    void exatamenteNoIntervalo() {
        LocalDate hoje = LocalDate.now();
        Doador doador = doadorComUltimaDoacao(hoje.minusDays(INTERVALO));

        service.registrarDoacao(1L, hoje);

        assertEquals(hoje, doador.getDataUltimaDoacao());
        verify(repository).save(doador);
    }

    @Test
    @DisplayName("Um dia antes do intervalo (59 dias): lança exceção, informa 1 dia restante e não altera nem salva")
    void umDiaAntesDoIntervalo() {
        LocalDate hoje = LocalDate.now();
        LocalDate ultima = hoje.minusDays(INTERVALO - 1);
        Doador doador = doadorComUltimaDoacao(ultima);

        DoacaoForaDoIntervaloException ex = assertThrows(DoacaoForaDoIntervaloException.class,
                () -> service.registrarDoacao(1L, hoje));

        assertTrue(ex.getMessage().contains("Faltam 1 dia(s)"));
        assertEquals(ultima, doador.getDataUltimaDoacao());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Doação no mesmo dia da última: lança exceção")
    void mesmoDia() {
        LocalDate hoje = LocalDate.now();
        doadorComUltimaDoacao(hoje);

        assertThrows(DoacaoForaDoIntervaloException.class, () -> service.registrarDoacao(1L, hoje));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Data de doação anterior à última registrada: lança exceção")
    void dataAnteriorAUltima() {
        LocalDate hoje = LocalDate.now();
        doadorComUltimaDoacao(hoje);

        assertThrows(DoacaoForaDoIntervaloException.class,
                () -> service.registrarDoacao(1L, hoje.minusDays(10)));
        verify(repository, never()).save(any());
    }
}
