package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateReservationRequest;
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

    @Transactional(readOnly = true)
    public List<Reservation> getMyReservations(Long userId) {
        log.debug("Fetching reservations for user id: {}", userId);
        return reservationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Reservation getReservationById(Long id) {
        log.debug("Fetching reservation with id: {}", id);
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    @Transactional
    public Reservation createReservation(CreateReservationRequest request, Long userId) {
        log.debug("Creating new reservation for user id: {}", userId);

        // Pobierz encje z bazy danych
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        // Pobierz usługi i oblicz całkowitą cenę
        Set<ServiceOffer> services = new HashSet<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Long serviceId : request.getServiceIds()) {
            ServiceOffer service = serviceOfferRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
            services.add(service);
            totalPrice = totalPrice.add(service.getPrice());
        }

        // Utwórz rezerwację
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setEmployee(employee);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setServices(services);
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CREATED);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created successfully with id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Reservation confirmReservation(Long id, Long userId) {
        log.debug("Confirming reservation with id: {} by user id: {}", id, userId);

        Reservation reservation = getReservationById(id);

        // Sprawdź czy rezerwacja należy do użytkownika
        if (!reservation.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to confirm reservation {} that doesn't belong to them", userId, id);
            throw new BadRequestException("Not authorized to confirm this reservation");
        }

        // Sprawdź czy rezerwacja jest w statusie CREATED
        if (reservation.getStatus() != ReservationStatus.CREATED) {
            log.warn("Attempt to confirm reservation {} in invalid status: {}", id, reservation.getStatus());
            throw new BadRequestException("Reservation cannot be confirmed in current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation {} confirmed successfully by client", id);
        return updated;
    }

    @Transactional
    public Reservation approveReservation(Long id) {
        log.debug("Approving reservation with id: {}", id);

        Reservation reservation = getReservationById(id);

        // Sprawdź czy rezerwacja jest w statusie CONFIRMED_BY_CLIENT
        if (reservation.getStatus() != ReservationStatus.CONFIRMED_BY_CLIENT) {
            log.warn("Attempt to approve reservation {} in invalid status: {}", id, reservation.getStatus());
            throw new BadRequestException("Reservation cannot be approved in current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.APPROVED);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation {} approved successfully by admin", id);
        return updated;
    }
}
