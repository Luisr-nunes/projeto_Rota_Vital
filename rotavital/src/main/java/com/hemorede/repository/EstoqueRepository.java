package com.hemorede.repository;

import com.hemorede.domain.model.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    List<Estoque> findByHospitalId(Long hospitalId);
}
