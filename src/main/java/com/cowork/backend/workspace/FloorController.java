package com.cowork.backend.workspace;

import com.cowork.backend.workspace.dto.CreateFloorRequest;
import com.cowork.backend.workspace.dto.FloorDto;
import com.cowork.backend.workspace.dto.UpdateMapRequest;
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
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping("/workspaces/{workspaceId}/floors")
    public List<FloorDto> list(@PathVariable Long workspaceId) {
        return floorService.list(workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/floors")
    public ResponseEntity<FloorDto> create(@PathVariable Long workspaceId,
                                           @Valid @RequestBody CreateFloorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(floorService.create(workspaceId, request));
    }

    @PutMapping("/floors/{floorId}")
    public FloorDto rename(@PathVariable Long floorId, @Valid @RequestBody CreateFloorRequest request) {
        return floorService.rename(floorId, request);
    }

    @PutMapping("/floors/{floorId}/map")
    public FloorDto updateMap(@PathVariable Long floorId, @RequestBody UpdateMapRequest request) {
        return floorService.updateMap(floorId, request);
    }

    @DeleteMapping("/floors/{floorId}")
    public ResponseEntity<Void> delete(@PathVariable Long floorId) {
        floorService.delete(floorId);
        return ResponseEntity.noContent().build();
    }
}
