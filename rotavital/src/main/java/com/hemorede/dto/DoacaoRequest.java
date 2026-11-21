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
        @NotNull(message = "informe o doador") Long doadorId,
        @NotNull(message = "informe o tipo sanguíneo") TipoSanguineo tipoSanguineo,
        @NotNull(message = "informe o hemocomponente") HemoComponente hemocomponente,
        @NotNull(message = "informe a data da coleta") @PastOrPresent(message = "a data da coleta não pode estar no futuro") LocalDate dataColeta,
        @NotNull(message = "informe a validade") @Future(message = "a validade deve ser uma data futura") LocalDate validade
) {
}
