package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.DiasPrevisaoInvalidoException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaxaDescarteServiceTest {
    @Test
    void calculaProporcaoPorTipoEComponenteNaJanelaDeSeteDias() {
        BolsaRepository repo = mock(BolsaRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                bolsa(TipoSanguineo.A_POS, HemoComponente.HEMACIAS, StatusBolsa.DESCARTADA, 1),
                bolsa(TipoSanguineo.A_POS, HemoComponente.HEMACIAS, StatusBolsa.UTILIZADA, 4),
                bolsa(TipoSanguineo.A_POS, HemoComponente.HEMACIAS, StatusBolsa.DESCARTADA, 8),
                bolsa(TipoSanguineo.O_NEG, HemoComponente.PLASMA, StatusBolsa.DESCARTADA, 2)));
        IndicadoresService service = new IndicadoresService(repo, mock(RequisicaoRepository.class), null);

        var response = service.calcularTaxaDescarte(7);

        assertEquals(7, response.diasAnalisados());
        assertEquals(LocalDate.now().minusDays(6), response.inicioPeriodo());
        assertEquals(LocalDate.now(), response.fimPeriodo());
        assertEquals(2, response.taxas().size());
        var aPos = response.taxas().stream()
                .filter(t -> t.tipoSanguineo() == TipoSanguineo.A_POS).findFirst().orElseThrow();
        assertEquals(2, aPos.bolsasVencidasNoPeriodo());
        assertEquals(1, aPos.bolsasDescartadas());
        assertEquals(50.0, aPos.taxaPercentual());
    }

    @Test
    void retornaListaVaziaSemInventarTaxasQuandoNaoHaBolsasNoPeriodo() {
        BolsaRepository repo = mock(BolsaRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        IndicadoresService service = new IndicadoresService(repo, mock(RequisicaoRepository.class), null);
        assertTrue(service.calcularTaxaDescarte(7).taxas().isEmpty());
    }

    @Test
    void rejeitaPeriodoInvalido() {
        IndicadoresService service = new IndicadoresService(
                mock(BolsaRepository.class), mock(RequisicaoRepository.class), null);
        assertThrows(DiasPrevisaoInvalidoException.class, () -> service.calcularTaxaDescarte(0));
    }

    private Bolsa bolsa(TipoSanguineo tipo, HemoComponente componente, StatusBolsa status, int diasDesdeValidade) {
        return Bolsa.builder().tipoSanguineo(tipo).hemoComponente(componente)
                .dataColeta(LocalDate.now().minusDays(40))
                .dataValidade(LocalDate.now().minusDays(diasDesdeValidade))
                .status(status).build();
    }
}
