package com.hemorede.dto;

import java.util.List;

/**
 * Resposta de {@code POST /api/rotas/calcular}: a rota de menor custo
 * (Dijkstra manual, {@code GrafoRotas}) do Hemocentro até o hospital
 * solicitante da requisição, com o tempo estimado de deslocamento
 * calculado a partir de uma velocidade média configurável
 * ({@code hemorede.rota.velocidade-media-kmh}).
 *
 * @param requisicaoId          Identificador da requisição para a qual a rota foi calculada.
 * @param caminho               Lista ordenada dos nós do grafo que compõem a rota (ex.: ["N0", "N5", "N2"]).
 * @param distanciaTotalKm      Custo total da rota (proxy de distância em km simulados).
 * @param tempoEstimadoMinutos  Tempo estimado de deslocamento, em minutos.
 */
public record RotaCalculadaResponse(
        Long requisicaoId,
        List<String> caminho,
        double distanciaTotalKm,
        double tempoEstimadoMinutos) {
}
