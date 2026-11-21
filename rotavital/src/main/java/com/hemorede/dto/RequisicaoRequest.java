package com.hemorede.dto;

import com.hemorede.domain.enums.Prioridade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        @NotNull(message = "informe o hospital") Long hospitalId,
        @NotNull(message = "informe a prioridade") Prioridade prioridade,
        @NotEmpty(message = "informe ao menos um item") @Size(max = 20, message = "no máximo 20 itens por requisição") List<@Valid @NotNull ItemRequisicaoRequest> itens
) {
}
