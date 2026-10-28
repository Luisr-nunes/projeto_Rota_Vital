package com.hemorede.dto;

import java.time.LocalDateTime;

/**
 * Métricas de comunicação da API para o painel de rede (PI2-173).
 * Latências em milissegundos; nulas quando ainda não houve tráfego.
 */
public record MetricasRedeResponse(
        LocalDateTime calculadoEm,
        long totalRequisicoes,
        double requisicoesPorSegundo,
        Double latenciaP50Ms,
        Double latenciaP95Ms,
        long erros4xx,
        long erros5xx
) {
}
