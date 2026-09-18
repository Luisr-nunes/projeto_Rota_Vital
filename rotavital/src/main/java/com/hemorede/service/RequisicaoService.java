package com.hemorede.service;

import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
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

        // Se algum item não conseguir ser 100% alocado, a EstoqueInsuficienteException
        // propaga e o Spring reverte toda a transação (nenhuma bolsa fica
        // parcialmente reservada) — a requisição permanece com o status atual
        // (PENDENTE), coerente com a HU04. Um "save" explícito aqui antes de
        // relançar a exceção seria desfeito pelo próprio rollback, por isso
        // não é necessário.
        for (ItemRequisicao item : requisicao.getItens()) {
            alocacaoService.alocar(item);
        }

        requisicao.setStatus(StatusRequisicao.APROVADA);
        return requisicaoRepository.save(requisicao);
    }
}
