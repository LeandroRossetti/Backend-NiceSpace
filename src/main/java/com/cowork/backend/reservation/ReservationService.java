package com.cowork.backend.reservation;

import com.cowork.backend.common.BadRequestException;
import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.resource.SpaceResource;
import com.cowork.backend.resource.SpaceResourceRepository;
import com.cowork.backend.reservation.dto.AvailabilityElement;
import com.cowork.backend.reservation.dto.AvailabilityResponse;
import com.cowork.backend.reservation.dto.CreateReservationRequest;
import com.cowork.backend.reservation.dto.ReservationDto;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.team.Team;
import com.cowork.backend.team.TeamMember;
import com.cowork.backend.team.TeamMemberRepository;
import com.cowork.backend.team.TeamMemberStatus;
import com.cowork.backend.team.TeamRepository;
import com.cowork.backend.user.User;
import com.cowork.backend.workspace.Workspace;
import com.cowork.backend.workspace.WorkspaceRepository;
import com.cowork.backend.workspace.dto.MapElement;
import com.cowork.backend.workspace.dto.MapStructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SpaceResourceRepository resourceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuthHelper authHelper;
    private final ObjectMapper objectMapper;

    public ReservationService(ReservationRepository reservationRepository,
                              SpaceResourceRepository resourceRepository,
                              WorkspaceRepository workspaceRepository,
                              TeamRepository teamRepository,
                              TeamMemberRepository teamMemberRepository,
                              AuthHelper authHelper,
                              ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.workspaceRepository = workspaceRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.authHelper = authHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReservationDto create(CreateReservationRequest request) {
        User user = authHelper.currentUser();

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("La hora de fin debe ser posterior a la hora de inicio");
        }

        SpaceResource resource = resourceRepository.findByIdForUpdate(request.resourceId())
                .orElseThrow(() -> new NotFoundException("Recurso no encontrado"));

        if (!Boolean.TRUE.equals(resource.getActive())) {
            throw new ConflictException("El recurso no esta activo");
        }

        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new NotFoundException("Equipo no encontrado"));
            if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)) {
                throw new ForbiddenException("No perteneces a este equipo");
            }
        }

        long overlapping = reservationRepository.countOverlapping(
                resource.getId(), ReservationStatus.CANCELADA, request.startTime(), request.endTime());
        if (overlapping > 0) {
            throw new ConflictException("El recurso ya esta reservado en ese horario");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ReservationStatus.CONFIRMADA)
                .createdBy(user)
                .team(team)
                .build();

        try {
            return ReservationDto.from(reservationRepository.save(reservation));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("El recurso ya esta reservado en ese horario");
        }
    }

    public List<ReservationDto> mine() {
        User user = authHelper.currentUser();
        List<Long> teamIds = teamMemberRepository.findByUserIdAndStatus(user.getId(), TeamMemberStatus.ACTIVE).stream()
                .map(TeamMember::getTeam)
                .map(Team::getId)
                .toList();

        List<Reservation> reservations;
        if (teamIds.isEmpty()) {
            reservations = reservationRepository.findMineOwn(user.getId());
        } else {
            reservations = reservationRepository.findMineWithTeams(user.getId(), teamIds);
        }
        return reservations.stream().map(ReservationDto::from).toList();
    }

    public List<ReservationDto> forWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace no encontrado"));
        User user = authHelper.currentUser();
        if (!workspace.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permisos sobre este workspace");
        }
        return reservationRepository.findByWorkspace(workspaceId).stream()
                .map(ReservationDto::from)
                .toList();
    }

    @Transactional
    public ReservationDto cancel(Long id) {
        User user = authHelper.currentUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        boolean allowed = reservation.getCreatedBy().getId().equals(user.getId())
                || (reservation.getTeam() != null
                    && teamMemberRepository.existsByTeamIdAndUserIdAndStatus(reservation.getTeam().getId(), user.getId(), TeamMemberStatus.ACTIVE))
                || reservation.getResource().getWorkspace().getOwner().getId().equals(user.getId());

        if (!allowed) {
            throw new ForbiddenException("No tienes permisos para cancelar esta reserva");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            throw new ConflictException("La reserva ya no esta activa");
        }
        if (!reservation.getEndTime().isAfter(Instant.now())) {
            throw new ConflictException("No se puede cancelar una reserva finalizada");
        }

        reservation.setStatus(ReservationStatus.CANCELADA);
        return ReservationDto.from(reservationRepository.save(reservation));
    }

    public AvailabilityResponse availability(Long workspaceId, Instant start, Instant end) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace no encontrado"));

        MapStructure structure = parseMap(workspace.getMapStructure());
        Map<Long, SpaceResource> resources = resourceRepository.findByWorkspaceIdOrderByIdAsc(workspaceId).stream()
                .collect(Collectors.toMap(SpaceResource::getId, Function.identity()));
        Set<Long> occupied = new HashSet<>(reservationRepository.findOccupiedResourceIds(
                workspaceId, ReservationStatus.CANCELADA, start, end));

        List<AvailabilityElement> elements = structure.elements().stream()
                .filter(e -> resources.containsKey(e.resourceId()))
                .map(e -> {
                    SpaceResource r = resources.get(e.resourceId());
                    boolean available = Boolean.TRUE.equals(r.getActive()) && !occupied.contains(r.getId());
                    return new AvailabilityElement(
                            r.getId(), r.getNombre(), r.getCapacidad(), r.getTipo(),
                            e.x(), e.y(), e.w(), e.h(), available);
                })
                .toList();

        return new AvailabilityResponse(workspace.getId(), workspace.getNombre(), elements);
    }

    private MapStructure parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new MapStructure(List.of());
        }
        try {
            return objectMapper.readValue(json, MapStructure.class);
        } catch (JsonProcessingException e) {
            return new MapStructure(List.of());
        }
    }
}
