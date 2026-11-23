package com.hemorede.dto;

import com.hemorede.requisicao.application.dto.ItemRequisicaoRequest;

import com.hemorede.domain.enums.Prioridade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Solicitação de bolsas pelo perfil HOSPITAL: o hospital vem do token, nunca do corpo. */
public record HospitalRequisicaoRequest(
        @NotNull(message = "informe a prioridade") Prioridade prioridade,
        @NotEmpty(message = "informe ao menos um item") @Size(max = 20, message = "no máximo 20 itens por requisição") List<@Valid @NotNull ItemRequisicaoRequest> itens
) {
}
