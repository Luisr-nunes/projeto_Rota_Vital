package com.hemorede.dto;

import com.hemorede.domain.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Conta criada pelo hemocentro: HOSPITAL (com hospitalId) ou outro HEMOCENTRO. */
public record NovoUsuarioRequest(
        @NotBlank(message = "informe o nome") @Size(max = 120) String nome,
        @NotBlank(message = "informe o e-mail") @Email(message = "e-mail inválido") String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter de 8 a 72 caracteres") String senha,
        @NotNull(message = "informe o perfil") Perfil perfil,
        Long hospitalId
) {
}
