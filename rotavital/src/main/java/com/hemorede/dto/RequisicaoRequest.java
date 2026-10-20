package com.hemorede.dto;

import com.hemorede.domain.enums.Prioridade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corpo de entrada do POST /api/requisicoes (HU03).
 * <p>
 * Substitui o recebimento direto da entidade {@code Requisicao}: o cliente
 * informa apenas o hospital, a prioridade e os itens. Status
 * ({@code PENDENTE}) e data da solicitação são definidos pelo servidor.
 * </p>
 */
public record RequisicaoRequest(
        @NotNull Long hospitalId,
        @NotNull Prioridade prioridade,
        @NotEmpty List<@Valid @NotNull ItemRequisicaoRequest> itens
) {
}
