package com.cowork.backend.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    Optional<TeamInvitation> findByToken(String token);

    List<TeamInvitation> findByEmailAndStatus(String email, InvitationStatus status);
}
