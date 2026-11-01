package com.cowork.backend.amenity;

import com.cowork.backend.amenity.dto.AmenityDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    @Transactional(readOnly = true)
    public List<AmenityDto> all() {
        return amenityRepository.findAllByOrderByNombreAsc().stream()
                .map(AmenityDto::from)
                .toList();
    }
}
