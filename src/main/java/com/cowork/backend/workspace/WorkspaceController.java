package com.cowork.backend.workspace;

import com.cowork.backend.workspace.dto.CreateWorkspaceRequest;
import com.cowork.backend.workspace.dto.WorkspaceDto;
import com.cowork.backend.workspace.dto.WorkspaceUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/workspaces")
    public List<WorkspaceDto> all() {
        return workspaceService.all();
    }

    @GetMapping("/workspaces/{id}")
    public WorkspaceDto get(@PathVariable Long id) {
        return workspaceService.get(id);
    }

    @GetMapping("/locations/{locationId}/workspaces")
    public List<WorkspaceDto> listByLocation(@PathVariable Long locationId) {
        return workspaceService.listByLocation(locationId);
    }

    @PostMapping("/locations/{locationId}/workspaces")
    public ResponseEntity<WorkspaceDto> create(@PathVariable Long locationId,
                                               @Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.create(locationId, request));
    }

    @PutMapping("/workspaces/{id}")
    public WorkspaceDto update(@PathVariable Long id, @Valid @RequestBody WorkspaceUpdateRequest request) {
        return workspaceService.update(id, request);
    }

    @DeleteMapping("/workspaces/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workspaceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
