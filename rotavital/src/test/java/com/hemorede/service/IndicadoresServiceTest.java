package com.hemorede.service;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.StatusRota;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.domain.model.Rota;
import com.hemorede.indicadores.DadosIndicadoresSinteticos;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("Indicadores estatísticos e operacionais (HU07)")
class IndicadoresServiceTest {

    private final IndicadoresService indicadoresService = new IndicadoresService();

    @Test
    @DisplayName("Deve calcular os indicadores sobre a amostra sintética legada")
    void deveCalcularIndicadoresSinteticos() {
        var resposta = indicadoresService.calcular(
                5,
                DadosSinteticos.criarBolsasExemplo(),
                DadosIndicadoresSinteticos.criarAtendimentosExemplo()
        );

        assertEquals(12, resposta.estoque().totalDisponivel());
        assertEquals(3, resposta.estoque().porTipoSanguineo().get(TipoSanguineo.O_POS));
        assertEquals(1.5, resposta.estoque().mediaPorTipo());
        assertEquals(1.0, resposta.estoque().medianaPorTipo());
        assertEquals(1, resposta.estoque().minimoPorTipo());
        assertEquals(3, resposta.estoque().maximoPorTipo());
        assertEquals(0.71, resposta.estoque().desvioPadraoPorTipo());

        assertEquals(3, resposta.vencimento().quantidadeProximaDoVencimento());
        assertEquals(25.0, resposta.vencimento().percentualProximoDoVencimento());

        assertEquals(5, resposta.requisicoes().total());
        assertEquals(3, resposta.requisicoes().atendidas());
        assertEquals(65.0, resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
        assertEquals(2, resposta.requisicoes().porStatus().get(StatusRequisicao.APROVADA));
    }

    @Test
    @DisplayName("Deve calcular indicadores sobre entidades JPA de Bolsa e Requisição")
    void deveCalcularIndicadoresComEntidades() {
        // Rota concluída com tempo de 45 minutos (chegada prevista - solicitação)
        Rota rota = new Rota();
        rota.setStatus(StatusRota.CONCLUIDA);
        rota.setDataSaida(LocalDateTime.now().minusMinutes(60));
        rota.setDataChegadaPrevista(LocalDateTime.now().minusMinutes(15));

        Requisicao reqPendente = new Requisicao();
        reqPendente.setStatus(StatusRequisicao.PENDENTE);
        reqPendente.setPrioridade(Prioridade.URGENTE);

        Requisicao reqAprovada = new Requisicao();
        reqAprovada.setStatus(StatusRequisicao.APROVADA);
        reqAprovada.setPrioridade(Prioridade.URGENTE);

        Requisicao reqEntregue = new Requisicao();
        reqEntregue.setStatus(StatusRequisicao.ENTREGUE);
        reqEntregue.setPrioridade(Prioridade.NORMAL);
        reqEntregue.setDataSolicitacao(LocalDateTime.now().minusMinutes(60));
        reqEntregue.setRota(rota);

        List<Bolsa> bolsas = DadosSinteticos.criarBolsasExemplo();
        List<Requisicao> requisicoes = List.of(reqPendente, reqAprovada, reqEntregue);

        var resposta = indicadoresService.calcularComEntidades(5, bolsas, requisicoes);

        assertNotNull(resposta.geradoEm());
        assertEquals(12, resposta.estoque().totalDisponivel());
        assertEquals(3, resposta.requisicoes().total());
        assertEquals(2, resposta.requisicoes().atendidas()); // Aprovada + Entregue
        assertEquals(1, resposta.requisicoes().porStatus().get(StatusRequisicao.PENDENTE));
        assertEquals(1, resposta.requisicoes().porStatus().get(StatusRequisicao.APROVADA));
        assertEquals(1, resposta.requisicoes().porStatus().get(StatusRequisicao.ENTREGUE));
        assertEquals(45.0, resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
    }

    @Test
    @DisplayName("Deve calcular indicadores consumindo os repositórios JPA injetados")
    void deveCalcularConsumindoRepositoriosJPA() {
        BolsaRepository bolsaRepoMock = Mockito.mock(BolsaRepository.class);
        RequisicaoRepository reqRepoMock = Mockito.mock(RequisicaoRepository.class);

        when(bolsaRepoMock.findAll()).thenReturn(DadosSinteticos.criarBolsasExemplo());
        when(reqRepoMock.findAll()).thenReturn(List.of(new Requisicao(1L, null, LocalDateTime.now(), Prioridade.URGENTE, StatusRequisicao.PENDENTE, List.of(), null)));

        IndicadoresService serviceComRepos = new IndicadoresService(bolsaRepoMock, reqRepoMock);
        var resposta = serviceComRepos.calcular(5);

        assertEquals(12, resposta.estoque().totalDisponivel());
        assertEquals(1, resposta.requisicoes().total());
        assertEquals(1, resposta.requisicoes().porStatus().get(StatusRequisicao.PENDENTE));
        assertEquals(0, resposta.requisicoes().atendidas());
        assertNull(resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
    }

    @Test
    @DisplayName("Cenário 2 da HU07: Deve retornar estado vazio neutro quando o banco de dados estiver vazio")
    void deveTratarBancoVazioComRepositorios() {
        BolsaRepository bolsaRepoMock = Mockito.mock(BolsaRepository.class);
        RequisicaoRepository reqRepoMock = Mockito.mock(RequisicaoRepository.class);

        when(bolsaRepoMock.findAll()).thenReturn(List.of());
        when(reqRepoMock.findAll()).thenReturn(List.of());

        IndicadoresService serviceVazio = new IndicadoresService(bolsaRepoMock, reqRepoMock);
        var resposta = serviceVazio.calcular(5);

        assertEquals(0, resposta.estoque().totalDisponivel());
        assertEquals(0.0, resposta.estoque().mediaPorTipo());
        assertEquals(0.0, resposta.estoque().medianaPorTipo());
        assertEquals(0L, resposta.estoque().minimoPorTipo());
        assertEquals(0L, resposta.estoque().maximoPorTipo());
        assertEquals(0.0, resposta.estoque().desvioPadraoPorTipo());
        assertEquals(0.0, resposta.vencimento().percentualProximoDoVencimento());
        assertEquals(0, resposta.vencimento().quantidadeProximaDoVencimento());
        assertEquals(0, resposta.requisicoes().total());
        assertEquals(0, resposta.requisicoes().atendidas());
        assertNull(resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
    }

    @Test
    @DisplayName("Deve retornar valores neutros quando listas forem vazias")
    void deveTratarAmostraVazia() {
        var resposta = indicadoresService.calcular(5, List.of(), List.of());

        assertEquals(0, resposta.estoque().totalDisponivel());
        assertEquals(0.0, resposta.vencimento().percentualProximoDoVencimento());
        assertEquals(0, resposta.requisicoes().atendidas());
        assertNull(resposta.requisicoes().tempoMedioAtendimentoEmMinutos());
    }

    @Test
    @DisplayName("Deve rejeitar janela de vencimento negativa")
    void deveRejeitarJanelaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> indicadoresService.calcular(-1));
    }
}
