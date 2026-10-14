package com.cowork.backend.team.dto;

import com.cowork.backend.team.InvitationStatus;
import com.cowork.backend.team.TeamInvitation;

import java.time.Instant;

public record TeamInvitationDto(
        Long id,
        Long teamId,
        String teamName,
        String email,
        String token,
        InvitationStatus status,
        Instant createdAt
) {

    public static TeamInvitationDto from(TeamInvitation invitation) {
        return new TeamInvitationDto(
                invitation.getId(),
                invitation.getTeam().getId(),
                invitation.getTeam().getNombre(),
                invitation.getEmail(),
                invitation.getToken(),
                invitation.getStatus(),
                invitation.getCreatedAt());
    }
}
