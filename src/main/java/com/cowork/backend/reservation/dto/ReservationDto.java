package com.cowork.backend.reservation.dto;

import com.cowork.backend.reservation.Reservation;
import com.cowork.backend.reservation.ReservationStatus;

import java.time.Instant;

public record ReservationDto(
        Long id,
        Long resourceId,
        String resourceName,
        String resourceType,
        Long workspaceId,
        String workspaceName,
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
                r.getResource().getId(),
                r.getResource().getNombre(),
                r.getResource().getTipo().name(),
                r.getResource().getWorkspace().getId(),
                r.getResource().getWorkspace().getNombre(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStatus(),
                r.getCreatedBy().getId(),
                r.getCreatedBy().getNombre(),
                r.getTeam() != null ? r.getTeam().getId() : null,
                r.getTeam() != null ? r.getTeam().getNombre() : null);
    }
}
