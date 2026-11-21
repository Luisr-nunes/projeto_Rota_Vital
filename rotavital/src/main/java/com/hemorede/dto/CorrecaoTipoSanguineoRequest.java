package com.hemorede.dto;

import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.validation.constraints.NotNull;

/** Correção do tipo sanguíneo do doador pelo hemocentro (ex.: o doador marcou errado no cadastro). */
public record CorrecaoTipoSanguineoRequest(@NotNull(message = "informe o tipo sanguíneo") TipoSanguineo tipoSanguineo) {
}
