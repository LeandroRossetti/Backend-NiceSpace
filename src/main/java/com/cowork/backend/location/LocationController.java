package com.cowork.backend.location;

import com.cowork.backend.location.dto.CreateLocationRequest;
import com.cowork.backend.location.dto.LocationDto;
import com.cowork.backend.location.dto.UpdateLocationRequest;
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
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/mine")
    public List<LocationDto> mine() {
        return locationService.mine();
    }

    @GetMapping("/{id}")
    public LocationDto get(@PathVariable Long id) {
        return locationService.get(id);
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(@Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(request));
    }

    @PutMapping("/{id}")
    public LocationDto update(@PathVariable Long id, @Valid @RequestBody UpdateLocationRequest request) {
        return locationService.update(id, request);
    }
}
