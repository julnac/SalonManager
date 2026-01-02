package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.dto.response.ReservationDetailDto;
import pl.edu.salonmanager.salon_manager.model.dto.ReservationStatusCount;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

import java.time.LocalDate;
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

    @Query("SELECT new pl.edu.salonmanager.salon_manager.model.dto.response.ReservationDetailDto(" +
           "r.id, r.startTime, r.endTime, r.status, r.totalPrice, " +
           "u.firstName, u.lastName, u.email, " +
           "e.firstName, e.lastName) " +
           "FROM Reservation r JOIN r.user u JOIN r.employee e " +
           "WHERE r.status = :status")
    Page<ReservationDetailDto> findReservationDetailsWithUserAndEmployee(
        @Param("status") ReservationStatus status, Pageable pageable);

    @Query("SELECT r.status as status, COUNT(r) as count " +
           "FROM Reservation r GROUP BY r.status")
    List<ReservationStatusCount> countByStatus();

    // @Query przykład - Native SQL z filtrowaniem
    @Query(value = "SELECT * FROM reservations r " +
           "WHERE r.start_time BETWEEN :startDate AND :endDate " +
           "AND (:status IS NULL OR r.status = CAST(:status AS VARCHAR)) " +
           "ORDER BY r.start_time DESC", nativeQuery = true)
    Page<Reservation> findByDateRangeAndStatus(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") String status, Pageable pageable);

    // Paginacja dla istniejących metod
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Page<Reservation> findByEmployeeId(Long employeeId, Pageable pageable);

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
}
