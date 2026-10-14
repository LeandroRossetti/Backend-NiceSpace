package com.cowork.backend.workspace.dto;

import com.cowork.backend.workspace.Workspace;

import java.time.Instant;

public record WorkspaceDto(Long id, String nombre, Long ownerId, String ownerName, String mapStructure, Instant createdAt) {

    public static WorkspaceDto from(Workspace workspace) {
        return new WorkspaceDto(
                workspace.getId(),
                workspace.getNombre(),
                workspace.getOwner().getId(),
                workspace.getOwner().getNombre(),
                workspace.getMapStructure(),
                workspace.getCreatedAt());
    }
}
