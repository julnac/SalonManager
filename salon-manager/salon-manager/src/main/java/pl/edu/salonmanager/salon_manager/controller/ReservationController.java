package pl.edu.salonmanager.salon_manager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.model.dto.response.AvailabilityResponseDto;
import pl.edu.salonmanager.salon_manager.model.dto.response.ReservationDetailDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.service.AvailabilityService;
import pl.edu.salonmanager.salon_manager.service.ReservationService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservations", description = "Reservation management endpoints")
public class ReservationController {

    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;

    // GET /api/v1/reservations/availability
    @GetMapping("/availability")
    @Operation(summary = "Search availability", description = "Finds available time slots for selected services on a given date")
    public ResponseEntity<AvailabilityResponseDto> searchAvailability(
            @RequestParam LocalDate date,
            @RequestParam List<Long> serviceIds) {

        log.info("REST request to search availability for date: {} and services: {}", date, serviceIds);

        AvailabilityResponseDto result = availabilityService.findAvailableSlots(date, serviceIds);

        return ResponseEntity.ok(result);
    }

    // GET /api/v1/reservations/my
    @GetMapping("/my")
    @Operation(summary = "Get my reservations", description = "Returns all reservations for the current user")
    public ResponseEntity<List<ReservationDetailDto>> getMyReservations(
            @RequestParam Long userId) {

        log.info("REST request to get reservations for user: {}", userId);

        List<Reservation> reservations = reservationService.getMyReservations(userId);

        List<ReservationDetailDto> result = reservations.stream()
                .map(this::mapToDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // POST /api/v1/reservations
    @PostMapping
    @Operation(summary = "Create reservation", description = "Creates a new reservation")
    public ResponseEntity<ReservationDetailDto> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @RequestParam Long userId) {

        log.info("REST request to create reservation for user: {}", userId);

        try {
            Reservation saved = reservationService.createReservation(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
        } catch (RuntimeException e) {
            log.error("Error creating reservation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/v1/reservations/{id}/confirm
    @PutMapping("/{id}/confirm")
    @Operation(summary = "Confirm reservation", description = "Confirms a reservation by the client")
    public ResponseEntity<ReservationDetailDto> confirmReservation(
            @PathVariable Long id,
            @RequestParam Long userId) {

        log.info("REST request to confirm reservation {} by user {}", id, userId);

        try {
            Reservation updated = reservationService.confirmReservation(id, userId);
            return ResponseEntity.ok(mapToDto(updated));
        } catch (RuntimeException e) {
            log.error("Error confirming reservation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /api/v1/reservations/{id}/approve (ADMIN)
    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve reservation (ADMIN)", description = "Approves a reservation by admin")
    public ResponseEntity<ReservationDetailDto> approveReservation(@PathVariable Long id) {

        log.info("REST request to approve reservation: {}", id);

        try {
            Reservation updated = reservationService.approveReservation(id);
            return ResponseEntity.ok(mapToDto(updated));
        } catch (RuntimeException e) {
            log.error("Error approving reservation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Mapper Reservation → DTO
    private ReservationDetailDto mapToDto(Reservation reservation) {
        return new ReservationDetailDto(
                reservation.getId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getTotalPrice(),
                reservation.getUser().getFirstName(),
                reservation.getUser().getLastName(),
                reservation.getUser().getEmail(),
                reservation.getEmployee().getFirstName(),
                reservation.getEmployee().getLastName()
        );
    }
}
