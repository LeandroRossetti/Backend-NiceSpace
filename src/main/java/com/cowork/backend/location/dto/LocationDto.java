package com.cowork.backend.location.dto;

import com.cowork.backend.location.Location;

import java.time.Instant;

public record LocationDto(
        Long id,
        String nombre,
        Long ownerId,
        String ownerName,
        String address,
        String city,
        String province,
        String zipCode,
        Double latitude,
        Double longitude,
        String description,
        String contactEmail,
        String contactPhone,
        Boolean isActive,
        Instant createdAt
) {

    public static LocationDto from(Location location) {
        return new LocationDto(
                location.getId(),
                location.getNombre(),
                location.getOwner().getId(),
                location.getOwner().getNombre(),
                location.getAddress(),
                location.getCity(),
                location.getProvince(),
                location.getZipCode(),
                location.getLatitude(),
                location.getLongitude(),
                location.getDescription(),
                location.getContactEmail(),
                location.getContactPhone(),
                location.getIsActive(),
                location.getCreatedAt());
    }
}
