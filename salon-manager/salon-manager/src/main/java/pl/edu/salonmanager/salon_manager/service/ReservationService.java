package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.CreateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.ReservationRequest;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.UpdateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;
import pl.edu.salonmanager.salon_manager.repository.ReservationRepository;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final ReservationSecurityService securityService;
    private final AvailabilityService availabilityService;

    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations(ReservationStatus status) {
        log.debug("Fetching all reservations with status filter: {}", status);
        if (status == null) {
            return reservationRepository.findAll();
        }
        return reservationRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Reservation getReservationById(Long id) {
        log.debug("Fetching reservation with id: {}", id);
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reservation> getMyReservations(Long userId) {
        log.debug("Fetching reservations for user id: {}", userId);
        return reservationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching reservations for employee {} between {} and {}", employeeId, startDate, endDate);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        return reservationRepository.findByEmployeeAndStartTimeBetween(
                employee,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
    }

    @Transactional
    public Reservation createReservation(CreateReservationRequest request, Long userId) {
        log.debug("Creating new reservation for user id: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.CREATED);

        applyReservationDetails(reservation, request, null);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created successfully with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Reservation approveReservation(Long id) {
        log.debug("Approving reservation with id: {} by salon", id);

        Reservation reservation = getReservationById(id);

        if (reservation.getStatus() != ReservationStatus.CREATED) {
            log.warn("Attempt to approve reservation {} in invalid status: {}", id, reservation.getStatus());
            throw new BadRequestException("Reservation cannot be approved in current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.APPROVED_BY_SALON);
        Reservation updated = reservationRepository.save(reservation);

        // Force initialization of lazy-loaded relationships
        updated.getUser().getEmail();
        updated.getEmployee().getFirstName();

        log.info("Reservation {} approved successfully by salon", id);
        return updated;
    }

    @Transactional
    public Reservation confirmReservation(Long id, Long userId) {
        log.debug("Confirming reservation with id: {} by user id: {}", id, userId);

        Reservation reservation = getReservationById(id);

        if (!reservation.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to confirm reservation {} that doesn't belong to them", userId, id);
            throw new BadRequestException("Not authorized to confirm this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.APPROVED_BY_SALON) {
            log.warn("Attempt to confirm reservation {} in invalid status: {}", id, reservation.getStatus());
            throw new BadRequestException("Reservation cannot be confirmed in current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        Reservation updated = reservationRepository.save(reservation);

        // Force initialization of lazy-loaded relationships
        updated.getUser().getEmail();
        updated.getEmployee().getFirstName();

        log.info("Reservation {} confirmed successfully by client", id);
        return updated;
    }


    @Transactional
    public Reservation updateReservation(Long id, UpdateReservationRequest request) {
        log.debug("Updating reservation with id: {}", id);

        Reservation reservation = getReservationById(id);

        if (!securityService.canEditReservation(reservation, reservation.getUser())) {
            log.warn("Not authorized to update reservation: {}", id);
            throw new UnauthorizedException("Not authorized to update this reservation or reservation is already APPROVED_BY_SALON or CANCELLED");
        }

        applyReservationDetails(reservation, request, id);

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation {} updated successfully", id);
        return updated;
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, User currentUser) {
        log.debug("Cancelling reservation {} by user {}", reservationId, currentUser.getId());

        Reservation reservation = getReservationById(reservationId);

        if (!securityService.canCancelReservation(reservation, currentUser)) {
            log.warn("User {} not authorized to cancel reservation {}",
                    currentUser.getId(), reservationId);
            throw new UnauthorizedException("Not authorized to cancel this reservation or reservation status is already CANCELLED or CONFIRMED_BY_CLIENT");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);

        // Force initialization of lazy-loaded relationships
        updated.getUser().getEmail();
        updated.getEmployee().getFirstName();

        log.info("Reservation {} cancelled successfully by user {}", reservationId, currentUser.getId());
        return updated;
    }

    private void validateReservationTime(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxFuture = now.plusMonths(3);

        if (startTime.isBefore(now)) {
            throw new BadRequestException("Cannot create reservation in the past");
        }

        if (startTime.isAfter(maxFuture)) {
            throw new BadRequestException("Cannot create reservation more than 3 months ahead");
        }
    }

    private void applyReservationDetails(Reservation reservation, ReservationRequest request, Long excludeReservationId) {
        validateReservationTime(request.getStartTime());

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        ReservationCalculation calculation = calculateReservationDetails(request.getServiceIds());

        if (!availabilityService.isSlotAvailable(
                request.getEmployeeId(),
                request.getStartTime(),
                request.getServiceIds().stream().toList(),
                excludeReservationId)) {
            throw new BadRequestException("Selected time slot is not available");
        }

        LocalDateTime endTime = request.getStartTime().plusMinutes(calculation.totalDurationMinutes());

        reservation.setEmployee(employee);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(endTime);
        reservation.setServices(calculation.services());
        reservation.setTotalPrice(calculation.totalPrice());
    }

    private record ReservationCalculation(
        Set<ServiceOffer> services,
        BigDecimal totalPrice,
        int totalDurationMinutes
    ) {}

    private ReservationCalculation calculateReservationDetails(Set<Long> serviceIds) {

        Set<ServiceOffer> services = new HashSet<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalDurationMinutes = 0;

        for (Long serviceId : serviceIds) {
            ServiceOffer service = serviceOfferRepository.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
            services.add(service);
            totalPrice = totalPrice.add(service.getPrice());
            totalDurationMinutes += service.getDurationMinutes();
        }

        if (totalDurationMinutes <= 0) {
            log.warn("Invalid total duration: {} minutes", totalDurationMinutes);
            throw new BadRequestException("Total service duration must be greater than 0");
        }

        return new ReservationCalculation(services, totalPrice, totalDurationMinutes);
    }

}
