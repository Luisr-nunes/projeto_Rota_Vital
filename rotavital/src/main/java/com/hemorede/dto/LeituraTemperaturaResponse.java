package com.hemorede.dto;

/**
 * Leitura de temperatura da cadeia fria — payload definido em
 * {@code doc/telemetria.md} (seção 1).
 *
 * @param veiculoId    identificador do veículo ({@code Veiculo.id})
 * @param temperaturaC temperatura em graus Celsius (uma casa decimal)
 * @param timestamp    instante da leitura em ISO-8601 com fuso horário
 * @param status       {@code OK} dentro da faixa segura; {@code ALERTA} fora dela
 */
public record LeituraTemperaturaResponse(
        Long veiculoId,
        double temperaturaC,
        String timestamp,
        String status
) {
}
