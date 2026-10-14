package com.cowork.backend.team.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
        @NotBlank(message = "El nombre del equipo es obligatorio")
        String nombre
) {
}
