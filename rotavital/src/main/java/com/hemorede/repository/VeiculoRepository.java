package com.hemorede.repository;

import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    List<Veiculo> findByStatusAndTipoRefrigeracao(StatusVeiculo status, TipoRefrigeracao tipoRefrigeracao);
}
