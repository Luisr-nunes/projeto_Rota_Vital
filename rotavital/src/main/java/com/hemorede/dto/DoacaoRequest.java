package com.hemorede.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Corpo de entrada do POST /api/doacoes.
 */
public record DoacaoRequest(
        @NotNull Long doadorId,
        @NotNull TipoSanguineo tipoSanguineo,
        @NotNull HemoComponente hemocomponente,
        @NotNull @PastOrPresent LocalDate dataColeta,
        @NotNull @Future LocalDate validade
) {
}
