package com.hemorede.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro de hospital pelo hemocentro (POST /api/hospitais). */
public record HospitalRequest(
        @NotBlank(message = "informe o nome do hospital") @Size(max = 150, message = "o nome deve ter até 150 caracteres") String nome,
        @NotBlank(message = "informe o CNPJ")
        @Pattern(regexp = "[0-9]{2}[.]?[0-9]{3}[.]?[0-9]{3}/?[0-9]{4}-?[0-9]{2}", message = "CNPJ inválido; use 14 dígitos") String cnpj,
        @Size(max = 200, message = "o endereço deve ter até 200 caracteres") String endereco,
        @DecimalMin(value = "-90.0", message = "latitude inválida") @DecimalMax(value = "90.0", message = "latitude inválida") Double latitude,
        @DecimalMin(value = "-180.0", message = "longitude inválida") @DecimalMax(value = "180.0", message = "longitude inválida") Double longitude,
        @Size(max = 10, message = "o código do nó deve ter até 10 caracteres") String codigoNo
) {
}
