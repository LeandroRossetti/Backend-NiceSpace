package com.cowork.backend.workspace.dto;

import com.cowork.backend.workspace.WorkspaceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WorkspaceUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El tipo de espacio es obligatorio")
        WorkspaceType workspaceType,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        Integer capacity,

        @DecimalMin(value = "0.00", message = "El precio por hora no puede ser negativo")
        BigDecimal pricePerHour,

        @DecimalMin(value = "0.00", message = "El precio por día no puede ser negativo")
        BigDecimal pricePerDay,

        @DecimalMin(value = "0.00", message = "El precio por mes no puede ser negativo")
        BigDecimal pricePerMonth,

        @Min(value = 1, message = "El máximo de horas debe ser al menos 1")
        Integer maxHours,

        @Min(value = 1, message = "El máximo de días debe ser al menos 1")
        Integer maxDays,

        @Min(value = 1, message = "El máximo de meses debe ser al menos 1")
        Integer maxMonths,

        String currency,

        Boolean isActive,

        Long floorId
) {
}
