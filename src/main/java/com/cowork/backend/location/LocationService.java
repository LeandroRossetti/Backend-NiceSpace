package com.cowork.backend.location;

import com.cowork.backend.amenity.Amenity;
import com.cowork.backend.amenity.AmenityRepository;
import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.location.dto.CreateLocationRequest;
import com.cowork.backend.location.dto.LocationDto;
import com.cowork.backend.location.dto.LocationSearchFilter;
import com.cowork.backend.location.dto.UpdateLocationRequest;
import com.cowork.backend.reservation.ReservationRepository;
import com.cowork.backend.reservation.ReservationStatus;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import com.cowork.backend.workspace.Floor;
import com.cowork.backend.workspace.FloorRepository;
import com.cowork.backend.workspace.Workspace;
import com.cowork.backend.workspace.WorkspaceRepository;
import com.cowork.backend.workspace.dto.MapElement;
import com.cowork.backend.workspace.dto.MapStructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final FloorRepository floorRepository;
    private final AmenityRepository amenityRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ReservationRepository reservationRepository;
    private final AuthHelper authHelper;
    private final ObjectMapper objectMapper;

    public LocationService(LocationRepository locationRepository,
                           FloorRepository floorRepository,
                           AmenityRepository amenityRepository,
                           WorkspaceRepository workspaceRepository,
                           ReservationRepository reservationRepository,
                           AuthHelper authHelper,
                           ObjectMapper objectMapper) {
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.amenityRepository = amenityRepository;
        this.workspaceRepository = workspaceRepository;
        this.reservationRepository = reservationRepository;
        this.authHelper = authHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> mine() {
        User user = authHelper.currentUser();
        return locationRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(LocationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LocationDto> search(LocationSearchFilter filter) {
        List<Location> locations = locationRepository.findByIsActiveTrueOrderByCreatedAtDesc();

        if (filter.city() != null) {
            String city = filter.city().toLowerCase();
            locations = locations.stream()
                    .filter(l -> l.getCity() != null && l.getCity().toLowerCase().contains(city))
                    .toList();
        }

        if (filter.province() != null) {
            String province = filter.province().toLowerCase();
            locations = locations.stream()
                    .filter(l -> l.getProvince() != null && l.getProvince().toLowerCase().contains(province))
                    .toList();
        }

        if (filter.amenityIds() != null && !filter.amenityIds().isEmpty()) {
            Set<Long> required = new HashSet<>(filter.amenityIds());
            locations = locations.stream()
                    .filter(l -> l.getAmenities().stream()
                            .map(Amenity::getId)
                            .collect(Collectors.toSet())
                            .containsAll(required))
                    .toList();
        }

        if (filter.start() != null) {
            locations = filterByAvailability(locations, filter.start(), filter.end());
        }

        return locations.stream().map(LocationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public LocationDto get(Long id) {
        return LocationDto.from(findOrThrow(id));
    }

    @Transactional
    public LocationDto create(CreateLocationRequest request) {
        User user = authHelper.currentUser();
        if (user.getAccountType() != AccountType.OWNER) {
            throw new ForbiddenException("Solo los dueños de espacios pueden crear sedes");
        }
        Location location = Location.builder()
                .nombre(request.nombre())
                .owner(user)
                .address(request.address())
                .city(request.city())
                .province(request.province())
                .zipCode(request.zipCode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .description(request.description())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .imageUrl(request.imageUrl())
                .isActive(request.isActive() == null || request.isActive())
                .mapStructure(null)
                .build();
        location.setAmenities(resolveAmenities(request.amenityIds()));
        Location saved = locationRepository.save(location);
        floorRepository.save(Floor.builder()
                .location(saved)
                .nombre("Piso 1")
                .levelOrder(1)
                .mapStructure("{\"elements\":[]}")
                .build());
        return LocationDto.from(saved);
    }

    @Transactional
    public LocationDto update(Long id, UpdateLocationRequest request) {
        Location location = findOrThrow(id);
        assertOwner(location);
        location.setNombre(request.nombre());
        location.setAddress(request.address());
        location.setCity(request.city());
        location.setProvince(request.province());
        location.setZipCode(request.zipCode());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setDescription(request.description());
        location.setContactEmail(request.contactEmail());
        location.setContactPhone(request.contactPhone());
        location.setImageUrl(request.imageUrl());
        if (request.isActive() != null) {
            location.setIsActive(request.isActive());
        }
        location.setAmenities(resolveAmenities(request.amenityIds()));
        return LocationDto.from(locationRepository.save(location));
    }

    public Location findOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sede no encontrada"));
    }

    public void assertOwner(Location location) {
        User user = authHelper.currentUser();
        if (!location.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permisos sobre esta sede");
        }
    }

    private Set<Amenity> resolveAmenities(List<Long> amenityIds) {
        if (amenityIds == null || amenityIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> uniqueIds = new HashSet<>(amenityIds);
        List<Amenity> amenities = amenityRepository.findAllById(uniqueIds);
        if (amenities.size() != uniqueIds.size()) {
            throw new NotFoundException("Una o más amenities no existen");
        }
        return new HashSet<>(amenities);
    }

    /**
     * Deja solo las sedes que tienen al menos un espacio activo, libre en el rango
     * y posicionado en el mapa de un piso (es decir, reservable desde el detalle).
     */
    private List<Location> filterByAvailability(List<Location> locations, Instant start, Instant end) {
        if (locations.isEmpty()) {
            return locations;
        }
        List<Long> locationIds = locations.stream().map(Location::getId).toList();

        Map<Long, List<Workspace>> workspacesByLocation = workspaceRepository.findByLocationIdIn(locationIds).stream()
                .collect(Collectors.groupingBy(w -> w.getLocation().getId()));
        Map<Long, List<Floor>> floorsByLocation = floorRepository.findByLocationIdInOrderByLevelOrderAsc(locationIds).stream()
                .collect(Collectors.groupingBy(f -> f.getLocation().getId()));
        Set<Long> occupied = new HashSet<>(reservationRepository.findOccupiedWorkspaceIdsInLocations(
                locationIds, ReservationStatus.CANCELADA, start, end));

        return locations.stream()
                .filter(location -> {
                    Set<Long> positioned = positionedWorkspaceIds(
                            floorsByLocation.getOrDefault(location.getId(), List.of()), location);
                    return workspacesByLocation.getOrDefault(location.getId(), List.of()).stream()
                            .anyMatch(w -> Boolean.TRUE.equals(w.getIsActive())
                                    && !occupied.contains(w.getId())
                                    && positioned.contains(w.getId()));
                })
                .toList();
    }

    private Set<Long> positionedWorkspaceIds(List<Floor> floors, Location location) {
        Set<Long> ids = new HashSet<>();
        for (Floor floor : floors) {
            String json = floor.getMapStructure() != null ? floor.getMapStructure() : location.getMapStructure();
            for (MapElement element : parseMap(json).elements()) {
                if (element.workspaceId() != null) {
                    ids.add(element.workspaceId());
                }
            }
        }
        return ids;
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