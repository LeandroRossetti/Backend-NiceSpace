package com.cowork.backend.reservation.dto;

import com.cowork.backend.reservation.RentalType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateReservationRequest(
        @NotNull(message = "El espacio es obligatorio")
        Long workspaceId,

        @NotNull(message = "La hora de inicio es obligatoria")
        Instant startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        Instant endTime,

        @NotNull(message = "La modalidad de alquiler es obligatoria")
        RentalType rentalType,

        Long teamId
) {
}
