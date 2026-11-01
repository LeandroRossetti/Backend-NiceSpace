package com.cowork.backend.location.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateLocationRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String address,

        String city,

        String province,

        String zipCode,

        @Min(value = -90, message = "La latitud debe estar entre -90 y 90")
        @Max(value = 90, message = "La latitud debe estar entre -90 y 90")
        Double latitude,

        @Min(value = -180, message = "La longitud debe estar entre -180 y 180")
        @Max(value = 180, message = "La longitud debe estar entre -180 y 180")
        Double longitude,

        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
        String description,

        @Email(message = "El email de contacto no es válido")
        String contactEmail,

        @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
        String contactPhone,

        String imageUrl,

        Boolean isActive,

        List<Long> amenityIds
) {
}
