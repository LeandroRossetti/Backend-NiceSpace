package com.cowork.backend.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre
) {
}
