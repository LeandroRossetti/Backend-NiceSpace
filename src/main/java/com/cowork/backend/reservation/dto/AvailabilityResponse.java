package com.cowork.backend.reservation.dto;

import java.util.List;
import com.cowork.backend.workspace.dto.MapObject;

public record AvailabilityResponse(
        Long workspaceId,
        String workspaceName,
        Long floorId,
        String floorName,
        List<AvailabilityElement> elements,
        List<MapObject> objects
) {
}
