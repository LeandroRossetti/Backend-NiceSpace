package com.cowork.backend.user.dto;

import com.cowork.backend.user.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es valido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El tipo de cuenta es obligatorio")
        AccountType accountType
) {
}
