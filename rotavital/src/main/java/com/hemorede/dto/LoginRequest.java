package com.hemorede.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank(message = "informe o e-mail") @Size(max = 254, message = "e-mail muito longo") String email,
                           @NotBlank(message = "informe a senha") @Size(max = 72, message = "senha muito longa") String senha) {
}
