package com.cowork.backend.workspace.dto;

public record MapObject(
        String elementId,
        String kind,
        String label,
        Double x,
        Double y,
        Double w,
        Double h,
        Double rotation
) {
}
