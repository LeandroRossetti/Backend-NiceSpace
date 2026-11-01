package com.cowork.backend.workspace;

import com.cowork.backend.amenity.Amenity;
import com.cowork.backend.amenity.AmenityRepository;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.location.Location;
import com.cowork.backend.location.LocationService;
import com.cowork.backend.workspace.dto.CreateWorkspaceRequest;
import com.cowork.backend.workspace.dto.WorkspaceDto;
import com.cowork.backend.workspace.dto.WorkspaceUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final AmenityRepository amenityRepository;
    private final LocationService locationService;
    private final FloorService floorService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            AmenityRepository amenityRepository,
                            LocationService locationService,
                            FloorService floorService) {
        this.workspaceRepository = workspaceRepository;
        this.amenityRepository = amenityRepository;
        this.locationService = locationService;
        this.floorService = floorService;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> all() {
        return workspaceRepository.findAll().stream().map(WorkspaceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> listByLocation(Long locationId) {
        locationService.findOrThrow(locationId);
        return workspaceRepository.findByLocationIdOrderByIdAsc(locationId).stream()
                .map(WorkspaceDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDto get(Long id) {
        return WorkspaceDto.from(findOrThrow(id));
    }

    @Transactional
    public WorkspaceDto create(Long locationId, CreateWorkspaceRequest request) {
        Location location = locationService.findOrThrow(locationId);
        locationService.assertOwner(location);
        Floor floor = request.floorId() == null
                ? null
                : floorService.findOwnedFloor(locationId, request.floorId());
        Workspace workspace = Workspace.builder()
                .nombre(request.nombre())
                .location(location)
                .floor(floor)
                .workspaceType(request.workspaceType())
                .capacity(request.capacity())
                .pricePerHour(request.pricePerHour())
                .pricePerDay(request.pricePerDay())
                .pricePerMonth(request.pricePerMonth())
                .maxHours(request.maxHours() == null ? 24 : request.maxHours())
                .maxDays(request.maxDays() == null ? 30 : request.maxDays())
                .maxMonths(request.maxMonths() == null ? 12 : request.maxMonths())
                .currency(request.currency() == null ? "ARS" : request.currency())
                .isActive(request.isActive() == null || request.isActive())
                .build();
        workspace.setAmenities(resolveAmenities(request.amenityIds()));
        return WorkspaceDto.from(workspaceRepository.save(workspace));
    }

    @Transactional
    public WorkspaceDto update(Long id, WorkspaceUpdateRequest request) {
        Workspace workspace = findOrThrow(id);
        assertOwner(workspace);
        workspace.setNombre(request.nombre());
        workspace.setWorkspaceType(request.workspaceType());
        workspace.setCapacity(request.capacity());
        workspace.setPricePerHour(request.pricePerHour());
        workspace.setPricePerDay(request.pricePerDay());
        workspace.setPricePerMonth(request.pricePerMonth());
        if (request.maxHours() != null) {
            workspace.setMaxHours(request.maxHours());
        }
        if (request.maxDays() != null) {
            workspace.setMaxDays(request.maxDays());
        }
        if (request.maxMonths() != null) {
            workspace.setMaxMonths(request.maxMonths());
        }
        if (request.currency() != null) {
            workspace.setCurrency(request.currency());
        }
        if (request.isActive() != null) {
            workspace.setIsActive(request.isActive());
        }
        if (request.floorId() != null) {
            workspace.setFloor(floorService.findOwnedFloor(workspace.getLocation().getId(), request.floorId()));
        }
        workspace.setAmenities(resolveAmenities(request.amenityIds()));
        return WorkspaceDto.from(workspaceRepository.save(workspace));
    }

    @Transactional
    public void delete(Long id) {
        Workspace workspace = findOrThrow(id);
        assertOwner(workspace);
        workspaceRepository.delete(workspace);
    }

    public Workspace findOrThrow(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
    }

    public void assertOwner(Workspace workspace) {
        locationService.assertOwner(workspace.getLocation());
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
}
