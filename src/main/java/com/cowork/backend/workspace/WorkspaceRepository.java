package com.cowork.backend.workspace;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findAll();

    List<Workspace> findByLocationIdOrderByIdAsc(Long locationId);

    List<Workspace> findByLocationIdIn(Collection<Long> locationIds);

    List<Workspace> findByFloorIdOrderByIdAsc(Long floorId);

    Optional<Workspace> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Workspace w where w.id = :id")
    Optional<Workspace> findByIdForUpdate(@Param("id") Long id);
}
