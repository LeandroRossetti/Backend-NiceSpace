package com.cowork.backend.resource.dto;

import com.cowork.backend.resource.ResourceType;
import com.cowork.backend.resource.SpaceResource;

import java.math.BigDecimal;

public record SpaceResourceDto(
        Long id,
        Long workspaceId,
        Long floorId,
        String nombre,
        Integer capacidad,
        ResourceType tipo,
        Boolean active,
        BigDecimal hourlyPrice,
        BigDecimal dailyPrice,
        BigDecimal monthlyPrice,
        String currency
) {

    public static SpaceResourceDto from(SpaceResource resource) {
        return new SpaceResourceDto(
                resource.getId(),
                resource.getWorkspace().getId(),
                resource.getFloor().getId(),
                resource.getNombre(),
                resource.getCapacidad(),
                resource.getTipo(),
                resource.getActive(),
                resource.getHourlyPrice(),
                resource.getDailyPrice(),
                resource.getMonthlyPrice(),
                resource.getCurrency());
    }
}
