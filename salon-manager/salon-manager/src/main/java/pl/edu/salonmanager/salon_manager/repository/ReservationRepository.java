package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.response.ReservationDetailDto;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r " +
           "JOIN FETCH r.user " +
           "JOIN FETCH r.employee " +
           "WHERE r.id = :id")
    Optional<Reservation> findByIdWithUserAndEmployee(@Param("id") Long id);

    List<Reservation> findByUserId(Long userId);
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findByEmployeeAndStartTimeBetween(Employee employee, LocalDateTime startTime, LocalDateTime endTime);

    Page<Reservation> findByUserId(Long userId, Pageable pageable);
    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    @Query("SELECT r FROM Reservation r " +
           "WHERE r.employee.id = :employeeId " +
           "AND r.status != 'CANCELLED' " +
           "AND DATE(r.startTime) = :date " +
           "ORDER BY r.startTime ASC")
    List<Reservation> findActiveReservationsByEmployeeAndDate(
        @Param("employeeId") Long employeeId,
        @Param("date") LocalDate date
    );


    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.employee.id = :employeeId " +
           "AND r.status != 'CANCELLED' " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean hasOverlappingReservation(
        @Param("employeeId") Long employeeId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.id != :reservationId " +
           "AND r.employee.id = :employeeId " +
           "AND r.status != 'CANCELLED' " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    boolean hasOverlappingReservationExcluding(
        @Param("reservationId") Long reservationId,
        @Param("employeeId") Long employeeId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
