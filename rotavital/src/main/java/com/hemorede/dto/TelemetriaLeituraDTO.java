package com.hemorede.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Leitura de telemetria recebida em POST /api/v1/telemetria/leituras (PI2-168).
 * Também é o elemento da fila de ingestão. {@code timestamp} é opcional.
 */
public record TelemetriaLeituraDTO(
        @NotNull(message = "veiculoId é obrigatório") Long veiculoId,
        @NotNull(message = "temperaturaC é obrigatória") Double temperaturaC,
        OffsetDateTime timestamp
) {
}