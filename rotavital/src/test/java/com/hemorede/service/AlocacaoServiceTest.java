package com.hemorede.service;

import com.hemorede.algoritmos.GrafoRotas;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.exception.EstoqueCompativelInsuficienteException;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlocacaoServiceTest {

    @Mock
    private BolsaRepository bolsaRepository;
    @Mock
    private RequisicaoRepository requisicaoRepository;
    @Mock
    private RoteirizacaoService roteirizacaoService;

    private AlocacaoService service;

    @BeforeEach
    void configurar() {
        service = new AlocacaoService(
                bolsaRepository,
                requisicaoRepository,
                roteirizacaoService,
                new CompatibilidadeSanguineaService());
    }

    @Test
    void deveAlocarCompativeisPorFefoEIncluirRota() {
        Requisicao requisicao = requisicaoPendente(2, TipoSanguineo.O_POS);
        Bolsa oPos = bolsa(10L, TipoSanguineo.O_POS, 5);
        Bolsa oNeg = bolsa(11L, TipoSanguineo.O_NEG, 2);
        Bolsa incompativel = bolsa(12L, TipoSanguineo.A_POS, 1);

        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicao));
        when(bolsaRepository.findByHemoComponenteAndStatus(
                HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                .thenReturn(List.of(oPos, oNeg, incompativel));
        when(roteirizacaoService.calcularRotaMinima(requisicao))
                .thenReturn(new GrafoRotas.ResultadoRota(List.of("N0", "N1"), 7.5));
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.alocarRequisicao(1L);

        assertEquals(StatusRequisicao.ALOCADA, resposta.status());
        assertEquals(List.of(11L, 10L), resposta.bolsas().stream().map(b -> b.id()).toList());
        assertEquals(List.of("N0", "N1"), resposta.rota());
        assertEquals(7.5, resposta.custoTotal());
        assertEquals(StatusBolsa.RESERVADA, oNeg.getStatus());
        assertEquals(StatusBolsa.RESERVADA, oPos.getStatus());
        assertEquals(StatusBolsa.DISPONIVEL, incompativel.getStatus());
    }

    @Test
    void deveRetornar422QuandoNaoHouverBolsaCompativel() {
        Requisicao requisicao = requisicaoPendente(1, TipoSanguineo.O_POS);
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicao));
        when(roteirizacaoService.calcularRotaMinima(requisicao))
                .thenReturn(new GrafoRotas.ResultadoRota(List.of("N0", "N1"), 7.5));
        when(bolsaRepository.findByHemoComponenteAndStatus(
                HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                .thenReturn(List.of(bolsa(12L, TipoSanguineo.A_POS, 2)));

        assertThrows(NenhumaBolsaCompativelException.class,
                () -> service.alocarRequisicao(1L));
        verify(bolsaRepository, never()).saveAll(any());
    }

    @Test
    void deveRetornar409ComQuantidadeDisponivelQuandoEstoqueForInsuficiente() {
        Requisicao requisicao = requisicaoPendente(2, TipoSanguineo.O_POS);
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicao));
        when(roteirizacaoService.calcularRotaMinima(requisicao))
                .thenReturn(new GrafoRotas.ResultadoRota(List.of("N0", "N1"), 7.5));
        when(bolsaRepository.findByHemoComponenteAndStatus(
                HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                .thenReturn(List.of(bolsa(11L, TipoSanguineo.O_NEG, 2)));

        EstoqueCompativelInsuficienteException ex = assertThrows(
                EstoqueCompativelInsuficienteException.class,
                () -> service.alocarRequisicao(1L));

        assertEquals(2, ex.getQuantidadeNecessaria());
        assertEquals(1, ex.getQuantidadeDisponivel());
        verify(bolsaRepository, never()).saveAll(any());
    }

    @Test
    void deveIgnorarBolsaVencida() {
        Requisicao requisicao = requisicaoPendente(1, TipoSanguineo.O_POS);
        Bolsa vencida = bolsa(11L, TipoSanguineo.O_NEG, -1);
        Bolsa valida = bolsa(10L, TipoSanguineo.O_POS, 5);
        when(requisicaoRepository.findById(1L)).thenReturn(Optional.of(requisicao));
        when(roteirizacaoService.calcularRotaMinima(requisicao))
                .thenReturn(new GrafoRotas.ResultadoRota(List.of("N0", "N1"), 7.5));
        when(bolsaRepository.findByHemoComponenteAndStatus(
                HemoComponente.HEMACIAS, StatusBolsa.DISPONIVEL))
                .thenReturn(List.of(vencida, valida));
        when(requisicaoRepository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.alocarRequisicao(1L);

        assertEquals(List.of(10L), resposta.bolsas().stream().map(b -> b.id()).toList());
        assertEquals(StatusBolsa.DISPONIVEL, vencida.getStatus());
    }

    private Requisicao requisicaoPendente(int quantidade, TipoSanguineo tipo) {
        Requisicao requisicao = new Requisicao();
        requisicao.setId(1L);
        requisicao.setStatus(StatusRequisicao.PENDENTE);
        requisicao.setPrioridade(Prioridade.URGENTE);
        requisicao.setDataSolicitacao(LocalDateTime.now());

        ItemRequisicao item = new ItemRequisicao();
        item.setId(2L);
        item.setRequisicao(requisicao);
        item.setHemoComponente(HemoComponente.HEMACIAS);
        item.setTipoSanguineo(tipo);
        item.setQuantidade(quantidade);
        item.setBolsasAlocadas(new ArrayList<>());
        requisicao.setItens(List.of(item));
        return requisicao;
    }

    private Bolsa bolsa(Long id, TipoSanguineo tipo, int diasAteValidade) {
        return Bolsa.builder()
                .id(id)
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(tipo)
                .dataColeta(LocalDate.now().minusDays(2))
                .dataValidade(LocalDate.now().plusDays(diasAteValidade))
                .status(StatusBolsa.DISPONIVEL)
                .build();
    }
}
