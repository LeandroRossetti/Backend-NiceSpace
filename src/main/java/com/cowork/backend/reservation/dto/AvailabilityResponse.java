package com.cowork.backend.reservation.dto;

import java.util.List;

public record AvailabilityResponse(
        Long workspaceId,
        String workspaceName,
        List<AvailabilityElement> elements
) {
}
