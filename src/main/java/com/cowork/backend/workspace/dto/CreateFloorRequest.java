package com.cowork.backend.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFloorRequest(
        @NotBlank(message = "El nombre del piso es obligatorio")
        String nombre
) {
}
