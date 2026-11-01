package com.cowork.backend.amenity.dto;

import com.cowork.backend.amenity.Amenity;

public record AmenityDto(Long id, String nombre, String icono) {

    public static AmenityDto from(Amenity amenity) {
        return new AmenityDto(
                amenity.getId(),
                amenity.getNombre(),
                amenity.getIcono());
    }
}
