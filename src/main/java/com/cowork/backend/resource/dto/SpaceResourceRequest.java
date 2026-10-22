package com.cowork.backend.resource.dto;

import com.cowork.backend.resource.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record SpaceResourceRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        Integer capacidad,

        @NotNull(message = "El tipo es obligatorio")
        ResourceType tipo,

        Boolean active,

        Long floorId,

        @DecimalMin(value = "0.00", message = "El precio por hora no puede ser negativo")
        BigDecimal hourlyPrice,

        @DecimalMin(value = "0.00", message = "El precio por día no puede ser negativo")
        BigDecimal dailyPrice,

        @DecimalMin(value = "0.00", message = "El precio por mes no puede ser negativo")
        BigDecimal monthlyPrice,

        @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO de tres letras")
        String currency
) {
}
