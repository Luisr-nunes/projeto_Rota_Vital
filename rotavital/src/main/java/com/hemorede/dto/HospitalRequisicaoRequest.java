package com.hemorede.dto;

import com.hemorede.domain.enums.Prioridade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Solicitação de bolsas pelo perfil HOSPITAL: o hospital vem do token, nunca do corpo. */
public record HospitalRequisicaoRequest(
        @NotNull(message = "informe a prioridade") Prioridade prioridade,
        @NotEmpty(message = "informe ao menos um item") List<@Valid @NotNull ItemRequisicaoRequest> itens
) {
}
