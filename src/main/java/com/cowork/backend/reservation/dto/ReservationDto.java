package com.cowork.backend.reservation.dto;

import com.cowork.backend.reservation.RentalType;
import com.cowork.backend.reservation.Reservation;
import com.cowork.backend.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationDto(
        Long id,
        Long workspaceId,
        String workspaceName,
        String workspaceType,
        Long locationId,
        String locationName,
        RentalType rentalType,
        BigDecimal totalPrice,
        Instant startTime,
        Instant endTime,
        ReservationStatus status,
        Long createdById,
        String createdByName,
        Long teamId,
        String teamName
) {

    public static ReservationDto from(Reservation r) {
        return new ReservationDto(
                r.getId(),
                r.getWorkspace().getId(),
                r.getWorkspace().getNombre(),
                r.getWorkspace().getWorkspaceType().name(),
                r.getWorkspace().getLocation().getId(),
                r.getWorkspace().getLocation().getNombre(),
                r.getRentalType(),
                r.getTotalPrice(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStatus(),
                r.getCreatedBy().getId(),
                r.getCreatedBy().getNombre(),
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getTeam() != null ? r.getTeam().getNombre() : null);
    }
}
