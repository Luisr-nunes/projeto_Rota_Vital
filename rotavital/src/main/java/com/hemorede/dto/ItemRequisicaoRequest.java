package com.hemorede.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Item do corpo de entrada do POST /api/requisicoes (HU03).
 */
public record ItemRequisicaoRequest(
        @NotNull HemoComponente hemoComponente,
        @NotNull TipoSanguineo tipoSanguineo,
        @NotNull @Positive Integer quantidade
) {
}
