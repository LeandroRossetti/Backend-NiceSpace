package com.cowork.backend.workspace.dto;

import com.cowork.backend.workspace.Workspace;
import com.cowork.backend.workspace.WorkspaceType;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkspaceDto(
        Long id,
        String nombre,
        Long locationId,
        String locationName,
        String city,
        String province,
        String address,
        Long floorId,
        WorkspaceType workspaceType,
        Integer capacity,
        BigDecimal pricePerHour,
        BigDecimal pricePerDay,
        BigDecimal pricePerMonth,
        Integer maxHours,
        Integer maxDays,
        Integer maxMonths,
        String currency,
        Boolean isActive,
        Instant createdAt
) {

    public static WorkspaceDto from(Workspace workspace) {
        return new WorkspaceDto(
                workspace.getId(),
                workspace.getNombre(),
                workspace.getLocation().getId(),
                workspace.getLocation().getNombre(),
                workspace.getLocation().getCity(),
                workspace.getLocation().getProvince(),
                workspace.getLocation().getAddress(),
                workspace.getFloor() != null ? workspace.getFloor().getId() : null,
                workspace.getWorkspaceType(),
                workspace.getCapacity(),
                workspace.getPricePerHour(),
                workspace.getPricePerDay(),
                workspace.getPricePerMonth(),
                workspace.getMaxHours(),
                workspace.getMaxDays(),
                workspace.getMaxMonths(),
                workspace.getCurrency(),
                workspace.getIsActive(),
                workspace.getCreatedAt());
    }
}
