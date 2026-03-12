package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.response.AvailabilityResponseDto;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.UpdateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.response.ReservationDetailDto;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.CreateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
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
    private final UserRepository userRepository;

    // GET /api/v1/reservations/availability
    @GetMapping("/availability")
    @Operation(summary = "Search availability (PUBLIC)", description = "Finds available time slots for selected services on a given date")
    public ResponseEntity<AvailabilityResponseDto> searchAvailability(
            @RequestParam LocalDate date,
            @RequestParam List<Long> serviceIds) {

        log.info("REST request to search availability for date: {} and services: {}", date, serviceIds);

        AvailabilityResponseDto result = availabilityService.findAvailableSlots(date, serviceIds);

        return ResponseEntity.ok(result);
    }

    // GET /api/v1/reservations/my
    @GetMapping("/my")
    @Operation(summary = "Get my reservations (USER)", description = "Returns all reservations for the authenticated user")
    public ResponseEntity<List<ReservationDetailDto>> getMyReservations() {

        Long userId = getCurrentUserId();
        log.info("REST request to get reservations for authenticated user: {}", userId);

        List<Reservation> reservations = reservationService.getMyReservations(userId);

        List<ReservationDetailDto> result = reservations.stream()
                .map(this::mapToDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // POST /api/v1/reservations
    @PostMapping
    @Operation(summary = "Create reservation (USER)", description = "Creates a new reservation for the authenticated user")
    public ResponseEntity<ReservationDetailDto> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {

        Long userId = getCurrentUserId();
        log.info("REST request to create reservation for authenticated user: {}", userId);

        Reservation saved = reservationService.createReservation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    // PUT /api/v1/reservations/{id}/confirm
    @PutMapping("/{id}/confirm")
    @Operation(summary = "Confirm reservation (USER)", description = "Confirms a reservation by the authenticated user")
    public ResponseEntity<ReservationDetailDto> confirmReservation(@PathVariable Long id) {

        Long userId = getCurrentUserId();
        log.info("REST request to confirm reservation {} by authenticated user {}", id, userId);

        Reservation updated = reservationService.confirmReservation(id, userId);
        return ResponseEntity.ok(mapToDto(updated));
    }

    // PUT /api/v1/reservations/{id}/approve (ADMIN)
    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve reservation (ADMIN)", description = "Approves a reservation by admin")
    public ResponseEntity<ReservationDetailDto> approveReservation(@PathVariable Long id) {

        Reservation updated = reservationService.approveReservation(id);
        return ResponseEntity.ok(mapToDto(updated));
    }

    // GET /api/v1/reservations (ADMIN)
    @GetMapping
    @Operation(summary = "Get all reservations (ADMIN)", description = "Returns all reservations with optional status filter")
    public ResponseEntity<List<ReservationDetailDto>> getAllReservations(
            @RequestParam(required = false) ReservationStatus status) {

        List<Reservation> reservations = reservationService.getAllReservations(status);
        List<ReservationDetailDto> result = reservations.stream()
                .map(this::mapToDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // GET /api/v1/reservations/employee/{employeeId} (ADMIN)
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get reservations by employee (ADMIN)", description = "Returns all reservations for a specific employee with optional date range")
    public ResponseEntity<List<ReservationDetailDto>> getReservationsByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate) {

        log.info("REST request to get reservations for employee: {} from {} to {}", employeeId, startDate, endDate);

        List<Reservation> reservations;
        if (startDate != null && endDate != null) {
            reservations = reservationService.getReservationsByEmployeeAndDateRange(employeeId, startDate, endDate);
        } else if (startDate != null) {
            reservations = reservationService.getReservationsByEmployeeAndDateRange(employeeId, startDate, startDate.plusDays(30));
        } else {
            java.time.LocalDate today = java.time.LocalDate.now();
            reservations = reservationService.getReservationsByEmployeeAndDateRange(employeeId, today.minusYears(1), today.plusYears(1));
        }

        List<ReservationDetailDto> result = reservations.stream()
                .map(this::mapToDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    // PUT /api/v1/reservations/{id}/cancel (ADMIN + USER with ownership)
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel reservation (ADMIN or USER with ownership)",
               description = "Cancels a reservation. Admin can cancel any reservation, user can only cancel their own (except APPROVED ones)")
    public ResponseEntity<ReservationDetailDto> cancelReservation(@PathVariable Long id) {

        Long userId = getCurrentUserId();
        log.info("REST request to cancel reservation {} by {}", id, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Reservation updated = reservationService.cancelReservation(id, user);

        return ResponseEntity.ok(mapToDto(updated));
    }

    // PUT /api/v1/reservations/{id} (ADMIN + USER with ownership, not if APPROVED)
    @PutMapping("/{id}")
    @Operation(summary = "Update reservation (ADMIN or USER with ownership)",
               description = "Updates a reservation. Cannot update APPROVED reservations for users. Admin can update any reservation.")
    public ResponseEntity<ReservationDetailDto> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationRequest request) {

        Long userId = getCurrentUserId();
        log.info("REST request to update reservation {} by {}", id, userId);

        Reservation updated = reservationService.updateReservation(id, request);

        return ResponseEntity.ok(mapToDto(updated));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return user.getId();
    }

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
                reservation.getEmployee().getLastName(),
                reservation.getServicesIds()
        );
    }
}
