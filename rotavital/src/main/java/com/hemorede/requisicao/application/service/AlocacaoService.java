package com.hemorede.requisicao.application.service;

import com.hemorede.estoque.api.ReservaEstoque;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.expedicao.application.service.RoteirizacaoService;
import com.hemorede.expedicao.domain.algorithm.GrafoRotas;
import com.hemorede.requisicao.application.dto.AlocacaoResponse;
import com.hemorede.requisicao.domain.enums.StatusRequisicao;
import com.hemorede.requisicao.domain.model.ItemRequisicao;
import com.hemorede.requisicao.domain.model.Requisicao;
import com.hemorede.requisicao.infrastructure.repository.RequisicaoRepository;
import com.hemorede.domain.regras.RegraTransicaoRequisicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Orquestra a alocação da requisição e o cálculo de sua rota. */
@Service
public class AlocacaoService {

    private final RequisicaoRepository requisicaoRepository;
    private final RoteirizacaoService roteirizacaoService;
    private final ReservaEstoque reservaEstoque;

    public AlocacaoService(RequisicaoRepository requisicaoRepository,
                           RoteirizacaoService roteirizacaoService,
                           ReservaEstoque reservaEstoque) {
        this.requisicaoRepository = requisicaoRepository;
        this.roteirizacaoService = roteirizacaoService;
        this.reservaEstoque = reservaEstoque;
    }

    @Transactional
    public AlocacaoResponse alocarRequisicao(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + requisicaoId));

        if (!RegraTransicaoRequisicao.permitida(requisicao.getStatus(), StatusRequisicao.ALOCADA)) {
            throw new IllegalStateException("A requisição precisa estar pendente para ser alocada: " + requisicaoId);
        }
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            throw new IllegalStateException("A requisição não tem itens para alocar: " + requisicaoId);
        }

        // Calcula e salva a rota antes de reservar; o rollback protege o fluxo se faltar estoque.
        GrafoRotas.ResultadoRota rota = roteirizacaoService.calcularRotaMinima(requisicao);
        for (ItemRequisicao item : requisicao.getItens()) {
            reservar(item);
        }

        requisicao.setStatus(StatusRequisicao.ALOCADA);
        requisicaoRepository.save(requisicao);
        List<AlocacaoResponse.BolsaAlocada> bolsas = requisicao.getItens().stream()
                .flatMap(item -> item.getBolsasAlocadas().stream())
                .map(bolsa -> new AlocacaoResponse.BolsaAlocada(
                        bolsa.getId(), bolsa.getTipoSanguineo(), bolsa.getHemoComponente(), bolsa.getDataValidade()))
                .toList();
        return new AlocacaoResponse(requisicao.getId(), requisicao.getStatus(), bolsas,
                rota.caminho(), rota.custoTotal());
    }

    private void reservar(ItemRequisicao item) {
        if (item == null || item.getHemoComponente() == null || item.getTipoSanguineo() == null
                || item.getQuantidade() == null || item.getQuantidade() <= 0) {
            throw new IllegalStateException("Item de requisição inválido para alocação");
        }
        if (item.getBolsasAlocadas() == null) {
            item.setBolsasAlocadas(new ArrayList<>());
        }
        int quantidadeRestante = item.getQuantidade() - item.getBolsasAlocadas().size();
        if (quantidadeRestante > 0) {
            item.getBolsasAlocadas().addAll(reservaEstoque.reservar(
                    item.getHemoComponente(), item.getTipoSanguineo(), quantidadeRestante));
        }
    }
}
