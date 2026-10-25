package com.cowork.backend.user.dto;

import com.cowork.backend.user.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
        @NotBlank(message = "El token de Google es obligatorio")
        String idToken,

        @NotNull(message = "El tipo de cuenta es obligatorio")
        AccountType accountType
) {
}
