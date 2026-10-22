package com.cowork.backend.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findByWorkspaceIdOrderByLevelOrderAsc(Long workspaceId);

    Optional<Floor> findFirstByWorkspaceIdOrderByLevelOrderAsc(Long workspaceId);

    Optional<Floor> findTopByWorkspaceIdOrderByLevelOrderDesc(Long workspaceId);

    long countByWorkspaceId(Long workspaceId);
}
