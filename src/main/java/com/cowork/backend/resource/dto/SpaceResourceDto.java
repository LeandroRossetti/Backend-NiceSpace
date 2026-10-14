package com.cowork.backend.resource.dto;

import com.cowork.backend.resource.ResourceType;
import com.cowork.backend.resource.SpaceResource;

public record SpaceResourceDto(Long id, Long workspaceId, String nombre, Integer capacidad, ResourceType tipo, Boolean active) {

    public static SpaceResourceDto from(SpaceResource resource) {
        return new SpaceResourceDto(
                resource.getId(),
                resource.getWorkspace().getId(),
                resource.getNombre(),
                resource.getCapacidad(),
                resource.getTipo(),
                resource.getActive());
    }
}
