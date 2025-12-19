package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByEmployeeId(Long employeeId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    List<Reservation> findByEmployeeAndStartTimeBetween(Employee employee, LocalDateTime startTime, LocalDateTime endTime);

    List<Reservation> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);
}
