package com.cowork.backend.team;

import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.team.dto.CreateTeamRequest;
import com.cowork.backend.team.dto.InviteRequest;
import com.cowork.backend.team.dto.TeamDto;
import com.cowork.backend.team.dto.TeamInvitationDto;
import com.cowork.backend.team.dto.TeamMemberDto;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final AuthHelper authHelper;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       TeamInvitationRepository invitationRepository,
                       AuthHelper authHelper) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.invitationRepository = invitationRepository;
        this.authHelper = authHelper;
    }

    @Transactional
    public TeamDto create(CreateTeamRequest request) {
        User user = authHelper.currentUser();
        if (user.getAccountType() != AccountType.TENANT) {
            throw new ForbiddenException("Solo los inquilinos pueden crear equipos");
        }
        Team team = Team.builder().nombre(request.nombre()).owner(user).build();
        team = teamRepository.save(team);

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(user)
                .status(TeamMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());

        return new TeamDto(team.getId(), team.getNombre(), team.getOwner().getId(), team.getOwner().getNombre(), "OWNER");
    }

    @Transactional(readOnly = true)
    public List<TeamDto> mine() {
        User user = authHelper.currentUser();
        return teamMemberRepository.findByUserIdAndStatus(user.getId(), TeamMemberStatus.ACTIVE).stream()
                .map(m -> {
                    Team t = m.getTeam();
                    String role = t.getOwner().getId().equals(user.getId()) ? "OWNER" : "MEMBER";
                    return new TeamDto(t.getId(), t.getNombre(), t.getOwner().getId(), t.getOwner().getNombre(), role);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDto> members(Long teamId) {
        User user = authHelper.currentUser();
        Team team = findOrThrow(teamId);
        assertMember(team, user);
        return teamMemberRepository.findByTeamId(teamId).stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACTIVE)
                .map(m -> new TeamMemberDto(m.getUser().getId(), m.getUser().getNombre(), m.getUser().getEmail(), m.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationDto> myInvitations() {
        User user = authHelper.currentUser();
        return invitationRepository.findByEmailAndStatus(user.getEmail(), InvitationStatus.PENDING).stream()
                .map(TeamInvitationDto::from)
                .toList();
    }

    @Transactional
    public void invite(Long teamId, InviteRequest request) {
        User user = authHelper.currentUser();
        Team team = findOrThrow(teamId);
        if (!team.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo el dueño del equipo puede invitar miembros");
        }
        TeamInvitation invitation = TeamInvitation.builder()
                .team(team)
                .email(request.email())
                .token(UUID.randomUUID().toString())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        invitationRepository.save(invitation);
    }

    @Transactional
    public TeamDto accept(String token) {
        User user = authHelper.currentUser();
        TeamInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Invitacion no encontrada"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("La invitacion ya fue procesada");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new ConflictException("La invitacion ha expirado");
        }
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ForbiddenException("Esta invitacion no corresponde a tu cuenta");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        Team team = invitation.getTeam();
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)) {
            teamMemberRepository.save(TeamMember.builder()
                    .team(team)
                    .user(user)
                    .status(TeamMemberStatus.ACTIVE)
                    .joinedAt(Instant.now())
                    .build());
        }
        return new TeamDto(team.getId(), team.getNombre(), team.getOwner().getId(), team.getOwner().getNombre(), "MEMBER");
    }

    @Transactional
    public void decline(String token) {
        TeamInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Invitacion no encontrada"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("La invitacion ya fue procesada");
        }
        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
    }

    public Team findOrThrow(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipo no encontrado"));
    }

    public void assertMember(Team team, User user) {
        boolean member = teamMemberRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE);
        if (!member) {
            throw new ForbiddenException("No perteneces a este equipo");
        }
    }

    public boolean isActiveMember(Long teamId, Long userId) {
        return teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, userId, TeamMemberStatus.ACTIVE);
    }
}
