package com.cowork.backend.location;

import com.cowork.backend.common.ForbiddenException;
import com.cowork.backend.common.NotFoundException;
import com.cowork.backend.location.dto.CreateLocationRequest;
import com.cowork.backend.location.dto.LocationDto;
import com.cowork.backend.location.dto.UpdateLocationRequest;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import com.cowork.backend.workspace.Floor;
import com.cowork.backend.workspace.FloorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final FloorRepository floorRepository;
    private final AuthHelper authHelper;

    public LocationService(LocationRepository locationRepository,
                           FloorRepository floorRepository,
                           AuthHelper authHelper) {
        this.locationRepository = locationRepository;
        this.floorRepository = floorRepository;
        this.authHelper = authHelper;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> mine() {
        User user = authHelper.currentUser();
        return locationRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(LocationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationDto get(Long id) {
        return LocationDto.from(findOrThrow(id));
    }

    @Transactional
    public LocationDto create(CreateLocationRequest request) {
        User user = authHelper.currentUser();
        if (user.getAccountType() != AccountType.OWNER) {
            throw new ForbiddenException("Solo los dueños de espacios pueden crear sedes");
        }
        Location location = Location.builder()
                .nombre(request.nombre())
                .owner(user)
                .address(request.address())
                .city(request.city())
                .province(request.province())
                .zipCode(request.zipCode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .description(request.description())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .isActive(request.isActive() == null || request.isActive())
                .mapStructure(null)
                .build();
        Location saved = locationRepository.save(location);
        floorRepository.save(Floor.builder()
                .location(saved)
                .nombre("Piso 1")
                .levelOrder(1)
                .mapStructure("{\"elements\":[]}")
                .build());
        return LocationDto.from(saved);
    }

    @Transactional
    public LocationDto update(Long id, UpdateLocationRequest request) {
        Location location = findOrThrow(id);
        assertOwner(location);
        location.setNombre(request.nombre());
        location.setAddress(request.address());
        location.setCity(request.city());
        location.setProvince(request.province());
        location.setZipCode(request.zipCode());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setDescription(request.description());
        location.setContactEmail(request.contactEmail());
        location.setContactPhone(request.contactPhone());
        if (request.isActive() != null) {
            location.setIsActive(request.isActive());
        }
        return LocationDto.from(locationRepository.save(location));
    }

    public Location findOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sede no encontrada"));
    }

    public void assertOwner(Location location) {
        User user = authHelper.currentUser();
        if (!location.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permisos sobre esta sede");
        }
    }
}
