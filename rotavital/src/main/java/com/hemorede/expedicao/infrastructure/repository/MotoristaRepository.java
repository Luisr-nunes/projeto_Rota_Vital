package com.hemorede.expedicao.infrastructure.repository;

import com.hemorede.expedicao.domain.model.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {
}
