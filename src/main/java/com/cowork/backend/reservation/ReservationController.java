package com.cowork.backend.reservation;

import com.cowork.backend.common.BadRequestException;
import com.cowork.backend.reservation.dto.AvailabilityResponse;
import com.cowork.backend.reservation.dto.CreateReservationRequest;
import com.cowork.backend.reservation.dto.ReservationDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationDto> create(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @GetMapping("/reservations/mine")
    public List<ReservationDto> mine() {
        return reservationService.mine();
    }

    @GetMapping("/reservations/workspace/{workspaceId}")
    public List<ReservationDto> forWorkspace(@PathVariable Long workspaceId) {
        return reservationService.forWorkspace(workspaceId);
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationDto cancel(@PathVariable Long id) {
        return reservationService.cancel(id);
    }

    @GetMapping("/workspaces/{workspaceId}/availability")
    public AvailabilityResponse availability(@PathVariable Long workspaceId,
                                             @RequestParam String start,
                                             @RequestParam String end) {
        try {
            Instant startTime = Instant.parse(start);
            Instant endTime = Instant.parse(end);
            if (!endTime.isAfter(startTime)) {
                throw new BadRequestException("La hora de fin debe ser posterior a la hora de inicio");
            }
            return reservationService.availability(workspaceId, startTime, endTime);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato de fecha invalido. Usar ISO-8601 (ej. 2026-09-24T10:00:00Z)");
        }
    }
}
