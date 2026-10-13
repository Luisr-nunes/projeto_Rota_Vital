package com.hemorede.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDate;
import java.util.List;

/** Resposta consolidada da alocacao: bolsas reservadas e menor rota calculada. */
public record AlocacaoResponse(
        Long requisicaoId,
        StatusRequisicao status,
        List<BolsaAlocada> bolsas,
        List<String> rota,
        double custoTotal
) {
    public record BolsaAlocada(
            Long id,
            TipoSanguineo tipoSanguineo,
            HemoComponente hemoComponente,
            LocalDate dataValidade
    ) {
    }
}
