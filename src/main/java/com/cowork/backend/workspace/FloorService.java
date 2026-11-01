package com.cowork.backend.workspace;

import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.location.Location;
import com.cowork.backend.location.LocationService;
import com.cowork.backend.workspace.dto.CreateFloorRequest;
import com.cowork.backend.workspace.dto.FloorDto;
import com.cowork.backend.workspace.dto.UpdateMapRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FloorService {

    private final FloorRepository floorRepository;
    private final LocationService locationService;
    private final WorkspaceRepository workspaceRepository;

    public FloorService(FloorRepository floorRepository,
                        LocationService locationService,
                        WorkspaceRepository workspaceRepository) {
        this.floorRepository = floorRepository;
        this.locationService = locationService;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<FloorDto> list(Long locationId) {
        locationService.findOrThrow(locationId);
        return floorRepository.findByLocationIdOrderByLevelOrderAsc(locationId).stream()
                .map(FloorDto::from)
                .toList();
    }

    @Transactional
    public FloorDto create(Long locationId, CreateFloorRequest request) {
        Location location = locationService.findOrThrow(locationId);
        locationService.assertOwner(location);
        int nextOrder = floorRepository.findTopByLocationIdOrderByLevelOrderDesc(locationId)
                .map(Floor::getLevelOrder)
                .orElse(0) + 1;
        Floor floor = Floor.builder()
                .location(location)
                .nombre(request.nombre().trim())
                .levelOrder(nextOrder)
                .mapStructure("{\"elements\":[]}")
                .build();
        return FloorDto.from(floorRepository.save(floor));
    }

    @Transactional
    public FloorDto rename(Long floorId, CreateFloorRequest request) {
        Floor floor = findOrThrow(floorId);
        locationService.assertOwner(floor.getLocation());
        floor.setNombre(request.nombre().trim());
        return FloorDto.from(floorRepository.save(floor));
    }

    @Transactional
    public void delete(Long floorId) {
        Floor floor = findOrThrow(floorId);
        locationService.assertOwner(floor.getLocation());
        if (floorRepository.countByLocationId(floor.getLocation().getId()) <= 1) {
            throw new ConflictException("El edificio debe conservar al menos un piso");
        }
        if (!workspaceRepository.findByFloorIdOrderByIdAsc(floorId).isEmpty()) {
            throw new ConflictException("No se puede eliminar un piso que contiene espacios");
        }
        floorRepository.delete(floor);
    }

    @Transactional
    public FloorDto updateMap(Long floorId, UpdateMapRequest request) {
        Floor floor = findOrThrow(floorId);
        locationService.assertOwner(floor.getLocation());
        floor.setMapStructure(request.mapStructure());
        boolean firstFloor = floorRepository.findFirstByLocationIdOrderByLevelOrderAsc(floor.getLocation().getId())
                .map(first -> first.getId().equals(floorId))
                .orElse(false);
        if (firstFloor) {
            floor.getLocation().setMapStructure(request.mapStructure());
        }
        return FloorDto.from(floorRepository.save(floor));
    }

    @Transactional(readOnly = true)
    public Floor findOrThrow(Long floorId) {
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new NotFoundException("Piso no encontrado"));
    }

    @Transactional(readOnly = true)
    public Floor findFirstForLocation(Long locationId) {
        return floorRepository.findFirstByLocationIdOrderByLevelOrderAsc(locationId)
                .orElseThrow(() -> new NotFoundException("El edificio no tiene pisos configurados"));
    }

    @Transactional(readOnly = true)
    public Floor findOwnedFloor(Long locationId, Long floorId) {
        Floor floor = findOrThrow(floorId);
        if (!floor.getLocation().getId().equals(locationId)) {
            throw new NotFoundException("Piso no encontrado en esta sede");
        }
        return floor;
    }
}
