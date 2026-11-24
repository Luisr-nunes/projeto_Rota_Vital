package com.hemorede.estoque.application.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import com.hemorede.repository.BolsaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservaEstoqueServiceTest {

    private final BolsaRepository bolsaRepository = mock(BolsaRepository.class);
    private final ReservaEstoqueService service = new ReservaEstoqueService(
            bolsaRepository, new CompatibilidadeSanguineaService());

    @Test
    void reservaBolsasCompativeisPelaValidadeMaisProxima() {
        Bolsa venceDepois = bolsa(TipoSanguineo.A_POS, LocalDate.now().plusDays(8));
        Bolsa venceAntes = bolsa(TipoSanguineo.O_NEG, LocalDate.now().plusDays(3));
        Bolsa incompativel = bolsa(TipoSanguineo.B_POS, LocalDate.now().plusDays(1));
        when(bolsaRepository.findByHemoComponenteAndStatus(HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                .thenReturn(List.of(venceDepois, incompativel, venceAntes));

        List<Bolsa> reservadas = service.reservar(HemoComponente.HEMACIAS, TipoSanguineo.A_POS, 2);

        assertEquals(List.of(venceAntes, venceDepois), reservadas);
        assertEquals(StatusBolsa.RESERVADA, venceAntes.getStatus());
        assertEquals(StatusBolsa.RESERVADA, venceDepois.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, incompativel.getStatus());
        verify(bolsaRepository).saveAll(reservadas);
    }

    @Test
    void rejeitaReservaSemBolsaCompativel() {
        when(bolsaRepository.findByHemoComponenteAndStatus(HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                .thenReturn(List.of(bolsa(TipoSanguineo.B_POS, LocalDate.now().plusDays(2))));

        assertThrows(NenhumaBolsaCompativelException.class,
                () -> service.reservar(HemoComponente.HEMACIAS, TipoSanguineo.A_POS, 1));
        verify(bolsaRepository, never()).saveAll(anyList());
    }

    private Bolsa bolsa(TipoSanguineo tipo, LocalDate validade) {
        return Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(tipo)
                .dataColeta(LocalDate.now())
                .dataValidade(validade)
                .status(StatusBolsa.DISPONIVEL)
                .build();
    }
}
