package com.hemorede.dto;

import com.hemorede.domain.enums.Sexo;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Autocadastro do perfil DOADOR: conta + dados básicos para doar. */
public record CadastroDoadorRequest(
        @NotBlank(message = "informe o nome") @Size(max = 120) String nome,
        @NotBlank(message = "informe o e-mail") @Email(message = "e-mail inválido") String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha deve ter de 8 a 72 caracteres") String senha,
        @NotBlank @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", message = "CPF inválido") String cpf,
        @NotNull(message = "informe o tipo sanguíneo") TipoSanguineo tipoSanguineo,
        @NotNull(message = "informe o sexo") Sexo sexo,
        @NotNull(message = "informe a data de nascimento") @Past(message = "a data de nascimento deve estar no passado") LocalDate dataNascimento,
        @NotNull(message = "informe o peso") @DecimalMin(value = "1.0", message = "peso inválido") @DecimalMax(value = "400.0", message = "peso inválido") Double pesoKg,
        @Size(max = 20) String telefone
) {
}
