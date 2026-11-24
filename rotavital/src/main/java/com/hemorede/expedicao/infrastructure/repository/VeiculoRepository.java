package com.hemorede.expedicao.infrastructure.repository;

import com.hemorede.expedicao.domain.enums.StatusVeiculo;
import com.hemorede.expedicao.domain.enums.TipoRefrigeracao;
import com.hemorede.expedicao.domain.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    List<Veiculo> findByStatusAndTipoRefrigeracao(StatusVeiculo status, TipoRefrigeracao tipoRefrigeracao);
}
