package com.hemorede.dto;

import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDateTime;
import java.util.List;

public record RiscoDesabastecimentoResponse(
        int dias,
        LocalDateTime calculadoEm,
        List<RiscoPorTipo> riscos
) {
    public record RiscoPorTipo(
            TipoSanguineo tipoSanguineo,
            long estoqueAtual,
            double lambdaDiario,
            double probabilidadeDesabastecimento
    ) {
    }
}
