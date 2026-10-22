package com.hemorede.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Resultado da taxa de descarte no período analisado. */
public record TaxaDescarteResponse(
        int diasAnalisados,
        LocalDate inicioPeriodo,
        LocalDate fimPeriodo,
        LocalDateTime calculadoEm,
        List<TaxaPorTipoEComponente> taxas) {

    /** A taxa só é emitida para grupos que tiveram bolsas com validade no período. */
    public record TaxaPorTipoEComponente(
            TipoSanguineo tipoSanguineo,
            HemoComponente hemoComponente,
            long bolsasVencidasNoPeriodo,
            long bolsasDescartadas,
            double taxaPercentual) {
    }
}
