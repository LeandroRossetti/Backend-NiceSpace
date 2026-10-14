package com.cowork.backend.user.dto;

public record AuthResponse(String token, UserDto user) {
}
