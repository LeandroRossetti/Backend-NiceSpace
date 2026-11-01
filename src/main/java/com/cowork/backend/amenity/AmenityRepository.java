package com.cowork.backend.amenity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findAllByOrderByNombreAsc();
}
