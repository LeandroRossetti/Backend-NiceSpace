package com.cowork.backend.reservation.dto;

import com.cowork.backend.resource.ResourceType;

public record AvailabilityElement(
        Long resourceId,
        String nombre,
        Integer capacidad,
        ResourceType tipo,
        Double x,
        Double y,
        Double w,
        Double h,
        boolean available
) {
}
