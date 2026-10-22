package com.cowork.backend.reservation.dto;

import com.cowork.backend.resource.ResourceType;

import java.math.BigDecimal;

public record AvailabilityElement(
        Long resourceId,
        String nombre,
        Integer capacidad,
        ResourceType tipo,
        Double x,
        Double y,
        Double w,
        Double h,
        Double rotation,
        boolean available,
        BigDecimal hourlyPrice,
        BigDecimal dailyPrice,
        BigDecimal monthlyPrice,
        String currency
) {
}
