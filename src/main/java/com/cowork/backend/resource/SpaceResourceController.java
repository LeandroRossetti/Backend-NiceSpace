package com.cowork.backend.resource;

import com.cowork.backend.resource.dto.SpaceResourceDto;
import com.cowork.backend.resource.dto.SpaceResourceRequest;
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
public class SpaceResourceController {

    private final SpaceResourceService resourceService;

    public SpaceResourceController(SpaceResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/workspaces/{workspaceId}/resources")
    public List<SpaceResourceDto> list(@PathVariable Long workspaceId) {
        return resourceService.list(workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/resources")
    public ResponseEntity<SpaceResourceDto> create(@PathVariable Long workspaceId,
                                                   @Valid @RequestBody SpaceResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(workspaceId, request));
    }

    @PutMapping("/resources/{id}")
    public SpaceResourceDto update(@PathVariable Long id, @Valid @RequestBody SpaceResourceRequest request) {
        return resourceService.update(id, request);
    }

    @DeleteMapping("/resources/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
