package pl.edu.salonmanager.salon_manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pl.edu.salonmanager.salon_manager.model.entity.*;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReservationRepository reservationRepository;

    private User testUser;
    private Employee testEmployee;
    private ServiceOffer testService;

    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        entityManager.persist(userRole);

        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(testUser);

        testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setEmail("john@salon.com");
        entityManager.persist(testEmployee);

        testService = new ServiceOffer();
        testService.setName("Haircut");
        testService.setPrice(new BigDecimal("50.00"));
        testService.setDurationMinutes(30);
        entityManager.persist(testService);

        entityManager.flush();
    }

    @Test
    void shouldSaveReservation() {

        Reservation reservation = createReservation(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30),
                ReservationStatus.CREATED
        );

        Reservation saved = reservationRepository.save(reservation);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getEmployee()).isEqualTo(testEmployee);
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CREATED);
    }

    @Test
    void shouldFindReservationsByUserId() {
        Reservation reservation1 = createReservation(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30),
                ReservationStatus.CREATED
        );
        Reservation reservation2 = createReservation(
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusMinutes(30),
                ReservationStatus.CONFIRMED_BY_CLIENT
        );
        entityManager.persist(reservation1);
        entityManager.persist(reservation2);
        entityManager.flush();

        List<Reservation> found = reservationRepository.findByUserId(testUser.getId());

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Reservation::getStatus)
                .containsExactlyInAnyOrder(ReservationStatus.CREATED, ReservationStatus.CONFIRMED_BY_CLIENT);
    }

    @Test
    void shouldFindReservationsByStatus() {
        Reservation reservation1 = createReservation(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30),
                ReservationStatus.CREATED
        );
        Reservation reservation2 = createReservation(
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusMinutes(30),
                ReservationStatus.CREATED
        );
        Reservation reservation3 = createReservation(
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusMinutes(30),
                ReservationStatus.APPROVED_BY_SALON
        );
        entityManager.persist(reservation1);
        entityManager.persist(reservation2);
        entityManager.persist(reservation3);
        entityManager.flush();

        List<Reservation> created = reservationRepository.findByStatus(ReservationStatus.CREATED);

        assertThat(created).hasSize(2);
        assertThat(created).allMatch(r -> r.getStatus() == ReservationStatus.CREATED);
    }

    @Test
    void shouldDetectOverlappingReservation() {
        LocalDateTime existingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime existingEnd = existingStart.plusMinutes(30);
        Reservation existing = createReservation(existingStart, existingEnd, ReservationStatus.CREATED);
        entityManager.persist(existing);
        entityManager.flush();

        LocalDateTime newStart = existingStart.plusMinutes(15);
        LocalDateTime newEnd = existingStart.plusMinutes(45);
        boolean hasOverlap = reservationRepository.hasOverlappingReservation(
                testEmployee.getId(), newStart, newEnd
        );

        assertThat(hasOverlap).isTrue();
    }

    @Test
    void shouldNotDetectOverlappingWhenNoConflict() {
        LocalDateTime existingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime existingEnd = existingStart.plusMinutes(30);
        Reservation existing = createReservation(existingStart, existingEnd, ReservationStatus.CREATED);
        entityManager.persist(existing);
        entityManager.flush();

        LocalDateTime newStart = existingStart.plusHours(1);
        LocalDateTime newEnd = newStart.plusMinutes(30);
        boolean hasOverlap = reservationRepository.hasOverlappingReservation(
                testEmployee.getId(), newStart, newEnd
        );

        assertThat(hasOverlap).isFalse();
    }

    @Test
    void shouldExcludeCurrentReservationWhenCheckingOverlap() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);
        Reservation existing = createReservation(start, end, ReservationStatus.CREATED);
        entityManager.persist(existing);
        entityManager.flush();

        boolean hasOverlap = reservationRepository.hasOverlappingReservationExcluding(
                existing.getId(), testEmployee.getId(), start, end
        );

        assertThat(hasOverlap).isFalse();
    }

    @Test
    void shouldFindReservationsByEmployeeAndDateRange() {
        LocalDateTime day1 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime day2 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime day5 = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0);

        Reservation res1 = createReservation(day1, day1.plusMinutes(30), ReservationStatus.CREATED);
        Reservation res2 = createReservation(day2, day2.plusMinutes(30), ReservationStatus.CREATED);
        Reservation res3 = createReservation(day5, day5.plusMinutes(30), ReservationStatus.CREATED);

        entityManager.persist(res1);
        entityManager.persist(res2);
        entityManager.persist(res3);
        entityManager.flush();

        List<Reservation> found = reservationRepository.findByEmployeeAndStartTimeBetween(
                testEmployee,
                day1.minusHours(1),
                day2.plusHours(12)
        );

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Reservation::getStartTime)
                .containsExactlyInAnyOrder(day1, day2);
    }

    @Test
    void shouldUpdateReservationStatus() {
        Reservation reservation = createReservation(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30),
                ReservationStatus.CREATED
        );
        entityManager.persist(reservation);
        entityManager.flush();
        entityManager.clear();

        Reservation found = reservationRepository.findById(reservation.getId()).orElseThrow();
        found.setStatus(ReservationStatus.CONFIRMED_BY_CLIENT);
        reservationRepository.save(found);
        entityManager.flush();
        entityManager.clear();

        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CONFIRMED_BY_CLIENT);
    }

    @Test
    void shouldDeleteReservation() {
        Reservation reservation = createReservation(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30),
                ReservationStatus.CREATED
        );
        entityManager.persist(reservation);
        entityManager.flush();
        Long reservationId = reservation.getId();

        reservationRepository.deleteById(reservationId);
        entityManager.flush();

        assertThat(reservationRepository.findById(reservationId)).isEmpty();
    }

    @Test
    void shouldFindAllReservations() {
        Reservation res1 = createReservation(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusMinutes(30),
                ReservationStatus.CREATED
        );
        Reservation res2 = createReservation(
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusMinutes(30),
                ReservationStatus.APPROVED_BY_SALON
        );
        entityManager.persist(res1);
        entityManager.persist(res2);
        entityManager.flush();

        List<Reservation> all = reservationRepository.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    private Reservation createReservation(LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setEmployee(testEmployee);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setServices(Set.of(testService));
        reservation.setTotalPrice(new BigDecimal("50.00"));
        reservation.setStatus(status);
        return reservation;
    }
}
