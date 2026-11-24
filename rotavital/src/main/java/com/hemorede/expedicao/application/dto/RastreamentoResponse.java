package com.hemorede.expedicao.application.dto;

import com.hemorede.requisicao.domain.enums.StatusRequisicao;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acompanhamento da entrega: trajeto (Dijkstra) com coordenadas, posição estimada do veículo,
 * trecho atual, percentual, horários, cadeia fria e link para o Google Maps.
 */
public record RastreamentoResponse(
        Long requisicaoId,
        StatusRequisicao status,
        String hospital,
        List<PontoTrajeto> trajeto,
        double distanciaKm,
        Posicao posicaoAtual,
        Integer trechoAtual,
        double percentual,
        LocalDateTime saida,
        LocalDateTime previsaoChegada,
        Long minutosRestantes,
        String veiculoPlaca,
        Double temperaturaC,
        String statusTemperatura,
        String googleMapsUrl
) {
    public record PontoTrajeto(String no, String nome, double lat, double lng) {
    }

    public record Posicao(double lat, double lng) {
    }
}
