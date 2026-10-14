package com.cowork.backend.workspace;

import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import com.cowork.backend.workspace.dto.CreateWorkspaceRequest;
import com.cowork.backend.workspace.dto.UpdateMapRequest;
import com.cowork.backend.workspace.dto.WorkspaceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final AuthHelper authHelper;

    public WorkspaceService(WorkspaceRepository workspaceRepository, AuthHelper authHelper) {
        this.workspaceRepository = workspaceRepository;
        this.authHelper = authHelper;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> all() {
        return workspaceRepository.findAll().stream().map(WorkspaceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> mine() {
        User user = authHelper.currentUser();
        return workspaceRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WorkspaceDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDto get(Long id) {
        return WorkspaceDto.from(findOrThrow(id));
    }

    @Transactional
    public WorkspaceDto create(CreateWorkspaceRequest request) {
        User user = authHelper.currentUser();
        if (user.getAccountType() != AccountType.OWNER) {
            throw new ForbiddenException("Solo los dueños de espacios pueden crear workspaces");
        }
        Workspace workspace = Workspace.builder()
                .nombre(request.nombre())
                .owner(user)
                .mapStructure(null)
                .build();
        return WorkspaceDto.from(workspaceRepository.save(workspace));
    }

    @Transactional
    public WorkspaceDto updateMap(Long id, UpdateMapRequest request) {
        Workspace workspace = findOrThrow(id);
        assertOwner(workspace);
        workspace.setMapStructure(request.mapStructure());
        return WorkspaceDto.from(workspaceRepository.save(workspace));
    }

    public Workspace findOrThrow(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workspace no encontrado"));
    }

    public void assertOwner(Workspace workspace) {
        User user = authHelper.currentUser();
        if (!workspace.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permisos sobre este workspace");
        }
    }
}
