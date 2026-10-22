package com.cowork.backend.workspace;

import com.cowork.backend.common.ConflictException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.resource.SpaceResourceRepository;
import com.cowork.backend.workspace.dto.CreateFloorRequest;
import com.cowork.backend.workspace.dto.FloorDto;
import com.cowork.backend.workspace.dto.UpdateMapRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FloorService {

    private final FloorRepository floorRepository;
    private final WorkspaceService workspaceService;
    private final SpaceResourceRepository resourceRepository;

    public FloorService(FloorRepository floorRepository,
                        WorkspaceService workspaceService,
                        SpaceResourceRepository resourceRepository) {
        this.floorRepository = floorRepository;
        this.workspaceService = workspaceService;
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<FloorDto> list(Long workspaceId) {
        workspaceService.findOrThrow(workspaceId);
        return floorRepository.findByWorkspaceIdOrderByLevelOrderAsc(workspaceId).stream()
                .map(FloorDto::from)
                .toList();
    }

    @Transactional
    public FloorDto create(Long workspaceId, CreateFloorRequest request) {
        Workspace workspace = workspaceService.findOrThrow(workspaceId);
        workspaceService.assertOwner(workspace);
        int nextOrder = floorRepository.findTopByWorkspaceIdOrderByLevelOrderDesc(workspaceId)
                .map(Floor::getLevelOrder)
                .orElse(0) + 1;
        Floor floor = Floor.builder()
                .workspace(workspace)
                .nombre(request.nombre().trim())
                .levelOrder(nextOrder)
                .mapStructure("{\"elements\":[]}")
                .build();
        return FloorDto.from(floorRepository.save(floor));
    }

    @Transactional
    public FloorDto rename(Long floorId, CreateFloorRequest request) {
        Floor floor = findOrThrow(floorId);
        workspaceService.assertOwner(floor.getWorkspace());
        floor.setNombre(request.nombre().trim());
        return FloorDto.from(floorRepository.save(floor));
    }

    @Transactional
    public void delete(Long floorId) {
        Floor floor = findOrThrow(floorId);
        workspaceService.assertOwner(floor.getWorkspace());
        if (floorRepository.countByWorkspaceId(floor.getWorkspace().getId()) <= 1) {
            throw new ConflictException("El edificio debe conservar al menos un piso");
        }
        if (!resourceRepository.findByFloorIdOrderByIdAsc(floorId).isEmpty()) {
            throw new ConflictException("No se puede eliminar un piso que contiene recursos");
        }
        floorRepository.delete(floor);
    }

    @Transactional
    public FloorDto updateMap(Long floorId, UpdateMapRequest request) {
        Floor floor = findOrThrow(floorId);
        workspaceService.assertOwner(floor.getWorkspace());
        floor.setMapStructure(request.mapStructure());
        boolean firstFloor = floorRepository.findFirstByWorkspaceIdOrderByLevelOrderAsc(floor.getWorkspace().getId())
                .map(first -> first.getId().equals(floorId))
                .orElse(false);
        if (firstFloor) {
            floor.getWorkspace().setMapStructure(request.mapStructure());
        }
        return FloorDto.from(floorRepository.save(floor));
    }

    @Transactional(readOnly = true)
    public Floor findOrThrow(Long floorId) {
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new NotFoundException("Piso no encontrado"));
    }

    @Transactional(readOnly = true)
    public Floor findFirstForWorkspace(Long workspaceId) {
        return floorRepository.findFirstByWorkspaceIdOrderByLevelOrderAsc(workspaceId)
                .orElseThrow(() -> new NotFoundException("El edificio no tiene pisos configurados"));
    }

    @Transactional(readOnly = true)
    public Floor findOwnedFloor(Long workspaceId, Long floorId) {
        Floor floor = findOrThrow(floorId);
        if (!floor.getWorkspace().getId().equals(workspaceId)) {
            throw new NotFoundException("Piso no encontrado en este edificio");
        }
        return floor;
    }
}
