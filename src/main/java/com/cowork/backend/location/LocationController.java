package com.cowork.backend.location;

import com.cowork.backend.common.BadRequestException;
import com.cowork.backend.location.dto.CreateLocationRequest;
import com.cowork.backend.location.dto.LocationDto;
import com.cowork.backend.location.dto.LocationSearchFilter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<LocationDto> all(@RequestParam(required = false) String city,
                                 @RequestParam(required = false) String province,
                                 @RequestParam(required = false) String amenityIds,
                                 @RequestParam(required = false) String start,
                                 @RequestParam(required = false) String end) {
        Instant startTime = parseInstant(start, "inicio");
        Instant endTime = parseInstant(end, "fin");

        if ((startTime == null) != (endTime == null)) {
            throw new BadRequestException("Para filtrar por disponibilidad indicá fecha de inicio y de fin");
        }
        if (startTime != null && !endTime.isAfter(startTime)) {
            throw new BadRequestException("La hora de fin debe ser posterior a la hora de inicio");
        }

        LocationSearchFilter filter = new LocationSearchFilter(
                normalize(city),
                normalize(province),
                parseAmenityIds(amenityIds),
                startTime,
                endTime);
        return locationService.search(filter);
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

    private Instant parseInstant(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    "Formato de fecha inválido para " + label + ". Usar ISO-8601 (ej. 2026-11-02T12:00:00Z)");
        }
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private List<Long> parseAmenityIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isEmpty())
                    .map(Long::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            throw new BadRequestException("amenityIds inválido: usar números separados por coma (ej. amenityIds=1,2)");
        }
    }
}
