package com.hemorede.requisicao.application.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Item do corpo de entrada do POST /api/requisicoes (HU03).
 */
public record ItemRequisicaoRequest(
        @NotNull(message = "informe o hemocomponente") HemoComponente hemoComponente,
        @NotNull(message = "informe o tipo sanguíneo") TipoSanguineo tipoSanguineo,
        @NotNull(message = "informe a quantidade") @Positive(message = "a quantidade deve ser maior que zero") @Max(value = 50, message = "a quantidade máxima por item é 50 bolsas") Integer quantidade
) {
}
