package com.cowork.backend.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    String FETCH = "select r from Reservation r "
            + "join fetch r.resource sr "
            + "join fetch sr.workspace w "
            + "join fetch r.createdBy u "
            + "left join fetch r.team t ";

    @Query(FETCH + "where r.createdBy.id = :userId order by r.startTime desc")
    List<Reservation> findMineOwn(@Param("userId") Long userId);

    @Query(FETCH + "where r.createdBy.id = :userId or r.team.id in :teamIds order by r.startTime desc")
    List<Reservation> findMineWithTeams(@Param("userId") Long userId, @Param("teamIds") Collection<Long> teamIds);

    @Query(FETCH + "where sr.workspace.id = :workspaceId order by r.startTime desc")
    List<Reservation> findByWorkspace(@Param("workspaceId") Long workspaceId);

    @Query("""
            select r.resource.id from Reservation r
            where r.resource.workspace.id = :workspaceId
              and r.status <> :cancelled
              and r.startTime < :end
              and r.endTime > :start
            """)
    List<Long> findOccupiedResourceIds(@Param("workspaceId") Long workspaceId,
                                       @Param("cancelled") ReservationStatus cancelled,
                                       @Param("start") Instant start,
                                       @Param("end") Instant end);

    @Query("""
            select count(r) from Reservation r
            where r.resource.id = :resourceId
              and r.status <> :cancelled
              and r.startTime < :end
              and r.endTime > :start
            """)
    long countOverlapping(@Param("resourceId") Long resourceId,
                          @Param("cancelled") ReservationStatus cancelled,
                          @Param("start") Instant start,
                          @Param("end") Instant end);
}
