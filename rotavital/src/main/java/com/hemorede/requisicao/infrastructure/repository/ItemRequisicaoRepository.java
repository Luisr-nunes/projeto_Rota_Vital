package com.hemorede.requisicao.infrastructure.repository;

import com.hemorede.requisicao.domain.model.ItemRequisicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRequisicaoRepository extends JpaRepository<ItemRequisicao, Long> {
}
