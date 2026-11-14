package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.exception.DiasPrevisaoInvalidoException;
import com.hemorede.indicadores.ModeloProbabilisticoDemanda;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrevisaoDemandaServiceTest {

    @Test
    void calculaMediaMovelIncluindoDiasSemRequisicao() {
        RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
        when(requisicoes.findAll()).thenReturn(List.of(requisicao(
                LocalDateTime.now().minusDays(2), StatusRequisicao.PENDENTE,
                HemoComponente.HEMACIAS, TipoSanguineo.O_POS, 3)));
        IndicadoresService service = service(requisicoes, lambdasReferencia(0.2));

        var resposta = service.calcularPrevisaoDemanda(7);
        var oPos = resposta.previsoes().stream()
                .filter(item -> item.tipoSanguineo() == TipoSanguineo.O_POS)
                .findFirst().orElseThrow();

        assertEquals(7, resposta.janelaHistoricaDias());
        assertEquals(3, oPos.bolsasObservadasNaJanela());
        assertEquals(0.43, oPos.lambdaDiario());
        assertEquals(3.0, oPos.demandaPrevistaNoHorizonte());
        assertEquals("MEDIA_MOVEL_7_DIAS", oPos.origem());
    }

    @Test
    void ignoraPedidosCanceladosForaDaJanelaEDeOutrosComponentes() {
        RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
        when(requisicoes.findAll()).thenReturn(List.of(
                requisicao(LocalDateTime.now().minusHours(1), StatusRequisicao.CANCELADA,
                        HemoComponente.HEMACIAS, TipoSanguineo.O_POS, 20),
                requisicao(LocalDateTime.now().minusDays(8), StatusRequisicao.ENTREGUE,
                        HemoComponente.HEMACIAS, TipoSanguineo.O_POS, 20),
                requisicao(LocalDateTime.now().minusHours(1), StatusRequisicao.ENTREGUE,
                        HemoComponente.PLAQUETAS, TipoSanguineo.O_POS, 20)));
        IndicadoresService service = service(requisicoes, lambdasReferencia(0.2));

        var oPos = service.calcularPrevisaoDemanda(7).previsoes().stream()
                .filter(item -> item.tipoSanguineo() == TipoSanguineo.O_POS)
                .findFirst().orElseThrow();

        assertEquals(0, oPos.bolsasObservadasNaJanela());
        assertEquals(0.2, oPos.lambdaDiario());
        assertEquals("LAMBDA_DE_REFERENCIA_PI2_177", oPos.origem());
    }

    @Test
    void usaMediaMovelComoLambdaNoCalculoDeRisco() {
        BolsaRepository bolsas = mock(BolsaRepository.class);
        RequisicaoRepository requisicoes = mock(RequisicaoRepository.class);
        when(bolsas.findAll()).thenReturn(List.of());
        when(requisicoes.findAll()).thenReturn(List.of(requisicao(
                LocalDateTime.now().minusDays(1), StatusRequisicao.APROVADA,
                HemoComponente.HEMACIAS, TipoSanguineo.O_POS, 7)));
        IndicadoresService service = new IndicadoresService(bolsas, requisicoes, modelo(lambdasReferencia(0.2)));

        var risco = service.calcularRiscoDesabastecimento(3).riscos().stream()
                .filter(item -> item.tipoSanguineo() == TipoSanguineo.O_POS)
                .findFirst().orElseThrow();

        assertEquals(1.0, risco.lambdaDiario());
    }

    @Test
    void rejeitaHorizonteForaDeUmASeteDias() {
        IndicadoresService service = service(mock(RequisicaoRepository.class), lambdasReferencia(0.2));

        assertThrows(DiasPrevisaoInvalidoException.class, () -> service.calcularPrevisaoDemanda(0));
        assertThrows(DiasPrevisaoInvalidoException.class, () -> service.calcularPrevisaoDemanda(8));
    }

    private IndicadoresService service(RequisicaoRepository requisicoes, Map<TipoSanguineo, Double> lambdas) {
        return new IndicadoresService(mock(BolsaRepository.class), requisicoes, modelo(lambdas));
    }

    private ModeloProbabilisticoDemanda modelo(Map<TipoSanguineo, Double> lambdas) {
        ModeloProbabilisticoDemanda modelo = new ModeloProbabilisticoDemanda();
        modelo.setLambdaDiario(lambdas);
        return modelo;
    }

    private Map<TipoSanguineo, Double> lambdasReferencia(double valor) {
        Map<TipoSanguineo, Double> lambdas = new EnumMap<>(TipoSanguineo.class);
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            lambdas.put(tipo, valor);
        }
        return lambdas;
    }

    private Requisicao requisicao(LocalDateTime data, StatusRequisicao status,
                                  HemoComponente componente, TipoSanguineo tipo, int quantidade) {
        ItemRequisicao item = new ItemRequisicao();
        item.setHemoComponente(componente);
        item.setTipoSanguineo(tipo);
        item.setQuantidade(quantidade);

        Requisicao requisicao = new Requisicao();
        requisicao.setDataSolicitacao(data);
        requisicao.setStatus(status);
        requisicao.setPrioridade(Prioridade.NORMAL);
        requisicao.setItens(List.of(item));
        return requisicao;
    }
}
