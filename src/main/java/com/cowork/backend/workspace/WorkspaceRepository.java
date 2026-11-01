package com.cowork.backend.workspace;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @EntityGraph(attributePaths = "amenities")
    List<Workspace> findAll();

    @EntityGraph(attributePaths = "amenities")
    List<Workspace> findByLocationIdOrderByIdAsc(Long locationId);

    @EntityGraph(attributePaths = "amenities")
    List<Workspace> findByFloorIdOrderByIdAsc(Long floorId);

    @EntityGraph(attributePaths = "amenities")
    Optional<Workspace> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Workspace w where w.id = :id")
    Optional<Workspace> findByIdForUpdate(@Param("id") Long id);
}
