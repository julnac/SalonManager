package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationSecurityService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean canEditReservation(Reservation reservation, User user) {

        if (userRepository.hasAdminRole(user.getId())) {
            return true;
        }

        return reservation.getUser().getId().equals(user.getId())
                && reservation.getStatus() != ReservationStatus.APPROVED
                && reservation.getStatus() != ReservationStatus.CANCELLED;
    }

    @Transactional(readOnly = true)
    public boolean canCancelReservation(Reservation reservation, User user) {

        if (userRepository.hasAdminRole(user.getId())) {
            return true;
        }

        return reservation.getUser().getId().equals(user.getId())
                && reservation.getStatus() != ReservationStatus.CANCELLED;
    }
}
