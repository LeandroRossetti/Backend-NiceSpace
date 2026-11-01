package com.cowork.backend.workspace.dto;

import com.cowork.backend.workspace.Floor;

public record FloorDto(Long id, Long locationId, String nombre, Integer levelOrder, String mapStructure) {

    public static FloorDto from(Floor floor) {
        return new FloorDto(
                floor.getId(),
                floor.getLocation().getId(),
                floor.getNombre(),
                floor.getLevelOrder(),
                floor.getMapStructure());
    }
}
