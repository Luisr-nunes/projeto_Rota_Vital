package com.hemorede.dto;

/**
 * Corpo da requisição de {@code POST /api/rotas/calcular}.
 *
 * @param requisicaoId Identificador da requisição hospitalar cujo hospital
 *                      solicitante será usado como destino do cálculo de rota.
 */
public record CalcularRotaRequest(Long requisicaoId) {
}