package com.cowork.backend.team.dto;

public record TeamDto(Long id, String nombre, Long ownerId, String ownerName, String myRole) {
}
