package com.cowork.backend.workspace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MapStructure(List<MapElement> elements, List<MapObject> objects) {

    public MapStructure {
        if (elements == null) {
            elements = List.of();
        }
        if (objects == null) {
            objects = List.of();
        }
    }

    public MapStructure(List<MapElement> elements) {
        this(elements, List.of());
    }
}
