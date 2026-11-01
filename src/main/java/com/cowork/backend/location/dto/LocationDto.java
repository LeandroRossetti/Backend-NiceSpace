package com.cowork.backend.location.dto;

import com.cowork.backend.amenity.dto.AmenityDto;
import com.cowork.backend.location.Location;

import java.time.Instant;
import java.util.List;

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
        String imageUrl,
        Boolean isActive,
        Instant createdAt,
        List<AmenityDto> amenities
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
                location.getImageUrl(),
                location.getIsActive(),
                location.getCreatedAt(),
                location.getAmenities().stream()
                        .map(AmenityDto::from)
                        .toList());
    }
}
