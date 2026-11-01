package com.cowork.backend.reservation;

import com.cowork.backend.common.BadRequestException;
import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.location.Location;
import com.cowork.backend.location.LocationService;
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
import com.cowork.backend.workspace.Floor;
import com.cowork.backend.workspace.FloorRepository;
import com.cowork.backend.workspace.Workspace;
import com.cowork.backend.workspace.WorkspaceRepository;
import com.cowork.backend.workspace.dto.MapStructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final FloorRepository floorRepository;
    private final LocationService locationService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuthHelper authHelper;
    private final ObjectMapper objectMapper;

    public ReservationService(ReservationRepository reservationRepository,
                              WorkspaceRepository workspaceRepository,
                              FloorRepository floorRepository,
                              LocationService locationService,
                              TeamRepository teamRepository,
                              TeamMemberRepository teamMemberRepository,
                              AuthHelper authHelper,
                              ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.workspaceRepository = workspaceRepository;
        this.floorRepository = floorRepository;
        this.locationService = locationService;
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

        Workspace workspace = workspaceRepository.findByIdForUpdate(request.workspaceId())
                .orElseThrow(() -> new NotFoundException("Espacio no encontrado"));

        if (!Boolean.TRUE.equals(workspace.getIsActive())) {
            throw new ConflictException("El espacio no está activo");
        }

        BigDecimal totalPrice = computeTotalPrice(workspace, request.rentalType(), request.startTime(), request.endTime());

        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new NotFoundException("Equipo no encontrado"));
            if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(team.getId(), user.getId(), TeamMemberStatus.ACTIVE)) {
                throw new ForbiddenException("No perteneces a este equipo");
            }
        }

        long overlapping = reservationRepository.countOverlapping(
                workspace.getId(), ReservationStatus.CANCELADA, request.startTime(), request.endTime());
        if (overlapping > 0) {
            throw new ConflictException("El espacio ya está reservado en ese horario");
        }

        Reservation reservation = Reservation.builder()
                .workspace(workspace)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .rentalType(request.rentalType())
                .totalPrice(totalPrice)
                .status(ReservationStatus.CONFIRMADA)
                .createdBy(user)
                .team(team)
                .build();

        try {
            return ReservationDto.from(reservationRepository.save(reservation));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("El espacio ya está reservado en ese horario");
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

    public List<ReservationDto> forLocation(Long locationId) {
        Location location = locationService.findOrThrow(locationId);
        locationService.assertOwner(location);
        return reservationRepository.findByLocation(locationId).stream()
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
                || reservation.getWorkspace().getLocation().getOwner().getId().equals(user.getId());

        if (!allowed) {
            throw new ForbiddenException("No tienes permisos para cancelar esta reserva");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            throw new ConflictException("La reserva ya no está activa");
        }
        if (!reservation.getEndTime().isAfter(Instant.now())) {
            throw new ConflictException("No se puede cancelar una reserva finalizada");
        }

        reservation.setStatus(ReservationStatus.CANCELADA);
        return ReservationDto.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long locationId, Long floorId, Instant start, Instant end) {
        Location location = locationService.findOrThrow(locationId);

        Floor floor = floorId == null
                ? floorRepository.findFirstByLocationIdOrderByLevelOrderAsc(locationId)
                    .orElseThrow(() -> new NotFoundException("El edificio no tiene pisos configurados"))
                : floorRepository.findById(floorId)
                    .filter(candidate -> candidate.getLocation().getId().equals(locationId))
                    .orElseThrow(() -> new NotFoundException("Piso no encontrado en esta sede"));

        MapStructure structure = parseMap(floor.getMapStructure() == null
                ? location.getMapStructure()
                : floor.getMapStructure());
        Map<Long, Workspace> workspaces = workspaceRepository.findByFloorIdOrderByIdAsc(floor.getId()).stream()
                .collect(Collectors.toMap(Workspace::getId, Function.identity()));
        Set<Long> occupied = new HashSet<>(reservationRepository.findOccupiedWorkspaceIds(
                locationId, ReservationStatus.CANCELADA, start, end));

        List<AvailabilityElement> elements = structure.elements().stream()
                .filter(e -> workspaces.containsKey(e.workspaceId()))
                .map(e -> {
                    Workspace w = workspaces.get(e.workspaceId());
                    boolean available = Boolean.TRUE.equals(w.getIsActive()) && !occupied.contains(w.getId());
                    return new AvailabilityElement(
                            w.getId(),
                            w.getNombre(),
                            w.getWorkspaceType(),
                            w.getCapacity(),
                            e.x(),
                            e.y(),
                            e.w(),
                            e.h(),
                            e.rotation() == null ? 0.0 : e.rotation(),
                            available,
                            w.getPricePerHour(),
                            w.getPricePerDay(),
                            w.getPricePerMonth(),
                            w.getMaxHours(),
                            w.getMaxDays(),
                            w.getMaxMonths(),
                            w.getCurrency());
                })
                .toList();

        return new AvailabilityResponse(
                location.getId(),
                location.getNombre(),
                floor.getId(),
                floor.getNombre(),
                location.getAddress(),
                location.getCity(),
                location.getProvince(),
                location.getZipCode(),
                location.getDescription(),
                location.getContactEmail(),
                location.getContactPhone(),
                location.getIsActive(),
                elements,
                structure.objects());
    }

    private BigDecimal computeTotalPrice(Workspace workspace, RentalType rentalType, Instant start, Instant end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            throw new BadRequestException("La duración debe ser mayor a cero");
        }
        double hours = minutes / 60.0;
        double days = minutes / (60.0 * 24.0);
        double months = days / 30.0;

        BigDecimal price;
        double quantity;
        switch (rentalType) {
            case HORA -> {
                if (hours > workspace.getMaxHours()) {
                    throw new BadRequestException("El alquiler por hora excede el máximo de " + workspace.getMaxHours() + " horas");
                }
                price = workspace.getPricePerHour();
                quantity = hours;
            }
            case DIA -> {
                if (days > workspace.getMaxDays()) {
                    throw new BadRequestException("El alquiler por día excede el máximo de " + workspace.getMaxDays() + " días");
                }
                price = workspace.getPricePerDay();
                quantity = days;
            }
            case MES -> {
                if (months > workspace.getMaxMonths()) {
                    throw new BadRequestException("El alquiler por mes excede el máximo de " + workspace.getMaxMonths() + " meses");
                }
                price = workspace.getPricePerMonth();
                quantity = months;
            }
            default -> throw new BadRequestException("Modalidad de alquiler inválida");
        }

        if (price == null) {
            throw new BadRequestException("Este espacio no admite alquiler por " + rentalType.name().toLowerCase());
        }
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private MapStructure parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new MapStructure(List.of(), List.of());
        }
        try {
            return objectMapper.readValue(json, MapStructure.class);
        } catch (JsonProcessingException e) {
            return new MapStructure(List.of(), List.of());
        }
    }
}
