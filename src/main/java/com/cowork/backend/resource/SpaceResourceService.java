package com.cowork.backend.resource;

import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.resource.dto.SpaceResourceDto;
import com.cowork.backend.resource.dto.SpaceResourceRequest;
import com.cowork.backend.workspace.Floor;
import com.cowork.backend.workspace.FloorService;
import com.cowork.backend.workspace.Workspace;
import com.cowork.backend.workspace.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpaceResourceService {

    private final SpaceResourceRepository resourceRepository;
    private final WorkspaceService workspaceService;
    private final FloorService floorService;

    public SpaceResourceService(SpaceResourceRepository resourceRepository,
                                WorkspaceService workspaceService,
                                FloorService floorService) {
        this.resourceRepository = resourceRepository;
        this.workspaceService = workspaceService;
        this.floorService = floorService;
    }

    @Transactional(readOnly = true)
    public List<SpaceResourceDto> list(Long workspaceId) {
        return resourceRepository.findByWorkspaceIdOrderByIdAsc(workspaceId).stream()
                .map(SpaceResourceDto::from)
                .toList();
    }

    @Transactional
    public SpaceResourceDto create(Long workspaceId, SpaceResourceRequest request) {
        Workspace workspace = workspaceService.findOrThrow(workspaceId);
        workspaceService.assertOwner(workspace);
        Floor floor = request.floorId() == null
                ? floorService.findFirstForWorkspace(workspaceId)
                : floorService.findOwnedFloor(workspaceId, request.floorId());
        SpaceResource resource = SpaceResource.builder()
                .workspace(workspace)
                .floor(floor)
                .nombre(request.nombre())
                .capacidad(request.capacidad())
                .tipo(request.tipo())
                .active(request.active() == null || request.active())
                .hourlyPrice(request.hourlyPrice())
                .dailyPrice(request.dailyPrice())
                .monthlyPrice(request.monthlyPrice())
                .currency(request.currency() == null ? "ARS" : request.currency())
                .build();
        return SpaceResourceDto.from(resourceRepository.save(resource));
    }

    @Transactional
    public SpaceResourceDto update(Long id, SpaceResourceRequest request) {
        SpaceResource resource = findOrThrow(id);
        workspaceService.assertOwner(resource.getWorkspace());
        resource.setNombre(request.nombre());
        resource.setCapacidad(request.capacidad());
        resource.setTipo(request.tipo());
        resource.setHourlyPrice(request.hourlyPrice());
        resource.setDailyPrice(request.dailyPrice());
        resource.setMonthlyPrice(request.monthlyPrice());
        resource.setCurrency(request.currency() == null ? "ARS" : request.currency());
        if (request.active() != null) {
            resource.setActive(request.active());
        }
        return SpaceResourceDto.from(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        SpaceResource resource = findOrThrow(id);
        workspaceService.assertOwner(resource.getWorkspace());
        resourceRepository.delete(resource);
    }

    public SpaceResource findOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recurso no encontrado"));
    }
}
