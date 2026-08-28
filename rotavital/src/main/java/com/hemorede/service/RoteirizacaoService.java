package com.hemorede.service;

import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.exception.VeiculoIncompativelException;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Regra de negócio 5: requisições urgentes têm prioridade de roteirização.
 * Regra de negócio 6: veículo só pode atender rota se a refrigeração for
 * compatível com o hemocomponente transportado.
 */
@Service
@RequiredArgsConstructor
public class RoteirizacaoService {

    private final RequisicaoRepository requisicaoRepository;
    private final VeiculoRepository veiculoRepository;

    /**
     * Retorna as requisições pendentes de roteirização, com as urgentes
     * primeiro (ordenação já vem do repository).
     */
    public List<Requisicao> proximasParaRoteirizar() {
        return requisicaoRepository
                .findByStatusOrderByPrioridadeAscDataSolicitacaoAsc(StatusRequisicao.APROVADA);
    }

    public Veiculo selecionarVeiculoCompativel(Requisicao requisicao) {
        var componente = requisicao.getItens().isEmpty()
                ? null
                : requisicao.getItens().get(0).getHemoComponente();

        if (componente == null) {
            throw new IllegalStateException("Requisição sem itens: " + requisicao.getId());
        }

        List<Veiculo> disponiveis = veiculoRepository.findByStatusAndTipoRefrigeracao(
                StatusVeiculo.DISPONIVEL, componente.getRefrigeracaoExigida());

        if (disponiveis.isEmpty()) {
            throw new VeiculoIncompativelException(
                    "Nenhum veículo disponível com refrigeração compatível com "
                            + componente + " para a requisição " + requisicao.getId());
        }

        // Em um cenário real, aqui entraria cálculo de distância até o hospital
        // solicitante (ex: fórmula de Haversine com lat/long) para escolher o
        // veículo mais próximo. Simplificado para o primeiro compatível.
        return disponiveis.get(0);
    }

    public boolean isUrgente(Requisicao requisicao) {
        return requisicao.getPrioridade() == Prioridade.URGENTE;
    }
}
