package com.hemorede.dto;

import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Resumo da previsão de demanda para cada tipo sanguíneo. */
public record PrevisaoDemandaResponse(
        int diasPrevistos,
        int janelaHistoricaDias,
        LocalDate inicioHistorico,
        LocalDate fimHistorico,
        LocalDateTime calculadaEm,
        List<PrevisaoPorTipo> previsoes
) {
    public record PrevisaoPorTipo(
            TipoSanguineo tipoSanguineo,
            long bolsasObservadasNaJanela,
            double lambdaDiario,
            double demandaPrevistaNoHorizonte,
            String origem
    ) {
    }
}
