package com.cowork.backend.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateReservationRequest(
        @NotNull(message = "El recurso es obligatorio")
        Long resourceId,

        @NotNull(message = "La hora de inicio es obligatoria")
        Instant startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        Instant endTime,

        Long teamId
) {
}
