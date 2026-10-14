package com.cowork.backend.resource;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceResourceRepository extends JpaRepository<SpaceResource, Long> {

    List<SpaceResource> findByWorkspaceIdOrderByIdAsc(Long workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sr from SpaceResource sr where sr.id = :id")
    Optional<SpaceResource> findByIdForUpdate(@Param("id") Long id);
}
