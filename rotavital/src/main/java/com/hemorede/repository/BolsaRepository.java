package com.hemorede.repository;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BolsaRepository extends JpaRepository<Bolsa, Long> {

    List<Bolsa> findByStatus(StatusBolsa status);

    // Usada pelo AlocacaoService para aplicar FEFO: ordena por validade crescente.
    List<Bolsa> findByHemoComponenteAndTipoSanguineoAndStatusOrderByDataValidadeAsc(
            HemoComponente hemoComponente, TipoSanguineo tipoSanguineo, StatusBolsa status);

    List<Bolsa> findByStatusAndDataValidadeBefore(StatusBolsa status, LocalDate data);

    long countByEstoqueIdAndTipoSanguineoAndHemoComponenteAndStatus(
            Long estoqueId, TipoSanguineo tipoSanguineo, HemoComponente hemoComponente, StatusBolsa status);
}
