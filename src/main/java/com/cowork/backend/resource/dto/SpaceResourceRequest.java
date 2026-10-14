package com.cowork.backend.resource.dto;

import com.cowork.backend.resource.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpaceResourceRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        Integer capacidad,

        @NotNull(message = "El tipo es obligatorio")
        ResourceType tipo,

        Boolean active
) {
}
