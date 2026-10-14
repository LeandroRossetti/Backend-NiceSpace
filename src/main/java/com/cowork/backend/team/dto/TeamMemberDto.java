package com.cowork.backend.team.dto;

import com.cowork.backend.team.TeamMemberStatus;

public record TeamMemberDto(Long userId, String nombre, String email, TeamMemberStatus status) {
}
