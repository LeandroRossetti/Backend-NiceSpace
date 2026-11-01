package com.cowork.backend.reservation.dto;

import com.cowork.backend.workspace.WorkspaceType;

import java.math.BigDecimal;

public record AvailabilityElement(
        Long workspaceId,
        String nombre,
        WorkspaceType workspaceType,
        Integer capacity,
        Double x,
        Double y,
        Double w,
        Double h,
        Double rotation,
        boolean available,
        BigDecimal pricePerHour,
        BigDecimal pricePerDay,
        BigDecimal pricePerMonth,
        Integer maxHours,
        Integer maxDays,
        Integer maxMonths,
        String currency
) {
}
