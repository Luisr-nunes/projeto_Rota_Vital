package com.hemorede.service;

import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.exception.EstoqueInsuficienteException;
import com.hemorede.repository.RequisicaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra de negócio 8: uma requisição só é aprovada quando todos os seus
 * itens conseguem ter a quantidade total de bolsas compatíveis alocadas.
 */
@Service
@RequiredArgsConstructor
public class RequisicaoService {

    private final RequisicaoRepository requisicaoRepository;
    private final AlocacaoService alocacaoService;

    @Transactional
    public Requisicao aprovar(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + requisicaoId));

        try {
            for (ItemRequisicao item : requisicao.getItens()) {
                alocacaoService.alocar(item);
            }
        } catch (EstoqueInsuficienteException ex) {
            // Mantém a requisição pendente se não for possível atender 100% dos itens.
            requisicao.setStatus(StatusRequisicao.PENDENTE);
            requisicaoRepository.save(requisicao);
            throw ex;
        }

        requisicao.setStatus(StatusRequisicao.APROVADA);
        return requisicaoRepository.save(requisicao);
    }
}
