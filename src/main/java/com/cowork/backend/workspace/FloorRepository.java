package com.cowork.backend.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findByLocationIdOrderByLevelOrderAsc(Long locationId);

    List<Floor> findByLocationIdInOrderByLevelOrderAsc(Collection<Long> locationIds);

    Optional<Floor> findFirstByLocationIdOrderByLevelOrderAsc(Long locationId);

    Optional<Floor> findTopByLocationIdOrderByLevelOrderDesc(Long locationId);

    long countByLocationId(Long locationId);
}
