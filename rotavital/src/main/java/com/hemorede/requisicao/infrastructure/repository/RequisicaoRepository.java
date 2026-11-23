package com.hemorede.requisicao.infrastructure.repository;

import com.hemorede.domain.enums.Prioridade;
import com.hemorede.requisicao.domain.enums.StatusRequisicao;
import com.hemorede.requisicao.domain.model.Requisicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisicaoRepository extends JpaRepository<Requisicao, Long> {
    List<Requisicao> findByStatusOrderByPrioridadeAscDataSolicitacaoAsc(StatusRequisicao status);
    List<Requisicao> findByPrioridadeAndStatus(Prioridade prioridade, StatusRequisicao status);

    // Perfil HOSPITAL: só as requisições do hospital do token.
    List<Requisicao> findByHospitalSolicitanteIdOrderByDataSolicitacaoDesc(Long hospitalId);
}
