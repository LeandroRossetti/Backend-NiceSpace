package com.cowork.backend.workspace;

import com.cowork.backend.workspace.dto.CreateWorkspaceRequest;
import com.cowork.backend.workspace.dto.UpdateMapRequest;
import com.cowork.backend.workspace.dto.WorkspaceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<WorkspaceDto> all() {
        return workspaceService.all();
    }

    @GetMapping("/mine")
    public List<WorkspaceDto> mine() {
        return workspaceService.mine();
    }

    @GetMapping("/{id}")
    public WorkspaceDto get(@PathVariable Long id) {
        return workspaceService.get(id);
    }

    @PostMapping
    public ResponseEntity<WorkspaceDto> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.create(request));
    }

    @PutMapping("/{id}/map")
    public WorkspaceDto updateMap(@PathVariable Long id, @RequestBody UpdateMapRequest request) {
        return workspaceService.updateMap(id, request);
    }
}
