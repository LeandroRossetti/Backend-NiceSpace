package com.cowork.backend.reservation.dto;

import com.cowork.backend.workspace.dto.MapObject;

import java.util.List;

public record AvailabilityResponse(
        Long locationId,
        String locationName,
        Long floorId,
        String floorName,
        String address,
        String city,
        String province,
        String zipCode,
        String description,
        String contactEmail,
        String contactPhone,
        Boolean isActive,
        List<AvailabilityElement> elements,
        List<MapObject> objects
) {
}
