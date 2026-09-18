package com.hemorede.dto;

import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Resposta consolidada dos indicadores estatísticos do Rota Vital.
 */
public record IndicadoresResponse(
        LocalDateTime geradoEm,
        EstoqueResumo estoque,
        VencimentoResumo vencimento,
        RequisicoesResumo requisicoes
) {

    public record EstoqueResumo(
            long totalDisponivel,
            Map<TipoSanguineo, Long> porTipoSanguineo,
            double mediaPorTipo,
            double medianaPorTipo,
            long minimoPorTipo,
            long maximoPorTipo,
            double desvioPadraoPorTipo
    ) {
    }

    public record VencimentoResumo(
            int janelaEmDias,
            long quantidadeProximaDoVencimento,
            double percentualProximoDoVencimento
    ) {
    }

    public record RequisicoesResumo(
            long total,
            long atendidas,
            Double tempoMedioAtendimentoEmMinutos,
            Map<StatusRequisicao, Long> porStatus
    ) {
    }
}
