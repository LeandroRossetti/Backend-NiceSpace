package com.cowork.backend.workspace.dto;

import com.cowork.backend.workspace.Floor;

public record FloorDto(Long id, Long workspaceId, String nombre, Integer levelOrder, String mapStructure) {

    public static FloorDto from(Floor floor) {
        return new FloorDto(
                floor.getId(),
                floor.getWorkspace().getId(),
                floor.getNombre(),
                floor.getLevelOrder(),
                floor.getMapStructure());
    }
}
