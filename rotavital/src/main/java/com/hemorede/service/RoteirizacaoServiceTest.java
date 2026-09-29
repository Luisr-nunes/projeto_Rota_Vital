package com.hemorede.service;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.algoritmos.GrafoRotas;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.exception.RotaIndisponivelException;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.repository.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Testes unitários (sem Spring) de {@link RoteirizacaoService#calcularRotaMinima(Requisicao)}
 * cobrindo os cenários da HU05, incluindo o cenário 2 (sem rota alcançável).
 */
@DisplayName("RoteirizacaoService - HU05 (rota mínima e cenário sem rota)")
class RoteirizacaoServiceTest {

    private static Requisicao requisicaoPara(String codigoNo) {
        Hospital hospital = new Hospital();
        hospital.setNome("Hospital Teste");
        hospital.setCodigoNo(codigoNo);
        Requisicao requisicao = new Requisicao();
        requisicao.setId(1L);
        requisicao.setHospitalSolicitante(hospital);
        return requisicao;
    }

    private static RoteirizacaoService servicoCom(GrafoRotas grafo) {
        return new RoteirizacaoService(mock(RequisicaoRepository.class), mock(VeiculoRepository.class), grafo);
    }

    @Test
    @DisplayName("Hospital com nó alcançável: retorna a rota mínima do Hemocentro")
    void rotaMinimaParaHospitalAlcancavel() {
        RoteirizacaoService service = servicoCom(DadosSinteticos.criarGrafoRotaVital());

        GrafoRotas.ResultadoRota rota = service.calcularRotaMinima(requisicaoPara("N5"));

        assertEquals(List.of("N0", "N5"), rota.caminho());
        assertEquals(10.0, rota.custoTotal(), 0.001);
    }

    @Test
    @DisplayName("Hospital sem aresta alcançável: lança RotaIndisponivelException (cenário 2)")
    void semRotaLancaRotaIndisponivel() {
        GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
        grafo.adicionarNo("N6");
        RoteirizacaoService service = servicoCom(grafo);

        RotaIndisponivelException ex = assertThrows(RotaIndisponivelException.class,
                () -> service.calcularRotaMinima(requisicaoPara("N6")));

        assertTrue(ex.getMessage().contains("N6"));
        assertTrue(ex.getMessage().contains("Hospital Teste"));
    }

    @Test
    @DisplayName("Hospital sem código de nó (nulo ou em branco): lança RotaIndisponivelException")
    void hospitalSemNoLancaRotaIndisponivel() {
        RoteirizacaoService service = servicoCom(DadosSinteticos.criarGrafoRotaVital());

        assertThrows(RotaIndisponivelException.class, () -> service.calcularRotaMinima(requisicaoPara(null)));
        assertThrows(RotaIndisponivelException.class, () -> service.calcularRotaMinima(requisicaoPara("  ")));
    }

    @Test
    @DisplayName("Requisição sem hospital solicitante: lança RotaIndisponivelException")
    void requisicaoSemHospitalLancaRotaIndisponivel() {
        RoteirizacaoService service = servicoCom(DadosSinteticos.criarGrafoRotaVital());
        Requisicao requisicao = new Requisicao();
        requisicao.setId(2L);

        assertThrows(RotaIndisponivelException.class, () -> service.calcularRotaMinima(requisicao));
    }

    @Test
    @DisplayName("Código de nó que não existe no grafo: propaga IllegalArgumentException do grafo")
    void noInexistenteNoGrafo() {
        RoteirizacaoService service = servicoCom(DadosSinteticos.criarGrafoRotaVital());

        assertThrows(IllegalArgumentException.class, () -> service.calcularRotaMinima(requisicaoPara("N99")));
    }
}
