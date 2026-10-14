package com.cowork.backend.workspace.dto;

import java.util.List;

public record MapStructure(List<MapElement> elements) {

    public MapStructure {
        if (elements == null) {
            elements = List.of();
        }
    }
}
