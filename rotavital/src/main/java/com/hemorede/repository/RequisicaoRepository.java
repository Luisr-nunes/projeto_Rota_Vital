package com.hemorede.repository;

import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.model.Requisicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {
    List<Requisicao> findByStatusOrderByPrioridadeAscDataSolicitacaoAsc(StatusRequisicao status);
    List<Requisicao> findByPrioridadeAndStatus(Prioridade prioridade, StatusRequisicao status);
}
