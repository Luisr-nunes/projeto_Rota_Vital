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
        @NotNull @PastOrPresent(message = "a data da coleta não pode estar no futuro") LocalDate dataColeta,
        @NotNull @Future(message = "a validade deve ser uma data futura") LocalDate validade
) {
}
