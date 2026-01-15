package pl.edu.salonmanager.salon_manager.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.edu.salonmanager.salon_manager.dao.StatisticsDao;
import pl.edu.salonmanager.salon_manager.model.dto.statistics.ClientStatisticsDto;
import pl.edu.salonmanager.salon_manager.model.entity.*;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(StatisticsDao.class)
class StatisticsDaoTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatisticsDao statisticsDao;

    private User testUser1;
    private User testUser2;
    private Employee testEmployee;
    private ServiceOffer testService;

    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        entityManager.persist(userRole);

        testUser1 = User.builder()
                .email("user1@example.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(testUser1);

        testUser2 = User.builder()
                .email("user2@example.com")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        entityManager.persist(testUser2);

        testEmployee = new Employee();
        testEmployee.setFirstName("Stylist");
        testEmployee.setLastName("Pro");
        testEmployee.setEmail("stylist@salon.com");
        entityManager.persist(testEmployee);

        testService = new ServiceOffer();
        testService.setName("Premium Service");
        testService.setPrice(new BigDecimal("100.00"));
        testService.setDurationMinutes(60);
        entityManager.persist(testService);

        entityManager.flush();
    }

    @Test
    void shouldGetClientStatistics() {
        createReservation(testUser1, LocalDateTime.now().plusDays(1), 60, ReservationStatus.CONFIRMED_BY_CLIENT);
        createReservation(testUser1, LocalDateTime.now().plusDays(2), 30, ReservationStatus.CONFIRMED_BY_CLIENT);
        createReservation(testUser2, LocalDateTime.now().plusDays(3), 45, ReservationStatus.CONFIRMED_BY_CLIENT);
        entityManager.flush();

        List<ClientStatisticsDto> statistics = statisticsDao.getClientStatistics();

        assertThat(statistics).hasSize(2);

        ClientStatisticsDto user1Stats = statistics.stream()
                .filter(s -> s.getClientEmail().equals("user1@example.com"))
                .findFirst()
                .orElseThrow();

        assertThat(user1Stats.getClientName()).isEqualTo("John Doe");
        assertThat(user1Stats.getTotalVisits()).isEqualTo(2);
    }

    @Test
    void shouldGetClientStatisticsById() {
        createReservation(testUser1, LocalDateTime.now().plusDays(1), 60, ReservationStatus.CONFIRMED_BY_CLIENT);
        createReservation(testUser1, LocalDateTime.now().plusDays(2), 30, ReservationStatus.CONFIRMED_BY_CLIENT);
        entityManager.flush();

        Optional<ClientStatisticsDto> stats = statisticsDao.getClientStatisticsById(testUser1.getId());

        assertThat(stats).isPresent();
        assertThat(stats.get().getClientEmail()).isEqualTo("user1@example.com");
        assertThat(stats.get().getTotalVisits()).isEqualTo(2);
        assertThat(stats.get().getAverageDurationMinutes()).isEqualTo(45); // (60 + 30) / 2
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoReservations() {

        Optional<ClientStatisticsDto> stats = statisticsDao.getClientStatisticsById(testUser1.getId());

        assertThat(stats).isEmpty();
    }

    @Test
    void shouldCalculateTotalSpending() {
        createReservation(testUser1, LocalDateTime.now().plusDays(1), 60, new BigDecimal("100.00"), ReservationStatus.CONFIRMED_BY_CLIENT);
        createReservation(testUser1, LocalDateTime.now().plusDays(2), 30, new BigDecimal("50.00"), ReservationStatus.CONFIRMED_BY_CLIENT);
        entityManager.flush();

        Optional<ClientStatisticsDto> stats = statisticsDao.getClientStatisticsById(testUser1.getId());

        assertThat(stats).isPresent();
        assertThat(stats.get().getTotalSpending()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(stats.get().getAverageSpending()).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void shouldCancelOldPendingReservations() {
        LocalDateTime cutoffDate = LocalDateTime.now();

        createReservation(testUser1, cutoffDate.minusDays(2), 30, ReservationStatus.CREATED);
        createReservation(testUser1, cutoffDate.minusDays(1), 30, ReservationStatus.CREATED);
        createReservation(testUser1, cutoffDate.plusDays(1), 30, ReservationStatus.CREATED);
        entityManager.flush();

        int cancelledCount = statisticsDao.cancelOldPendingReservations(cutoffDate);

        assertThat(cancelledCount).isEqualTo(2);
    }

    @Test
    void shouldNotIncludeCancelledReservationsInStatistics() {
        createReservation(testUser1, LocalDateTime.now().plusDays(1), 60, ReservationStatus.CONFIRMED_BY_CLIENT);
        createReservation(testUser1, LocalDateTime.now().plusDays(2), 30, ReservationStatus.CANCELLED);
        entityManager.flush();

        Optional<ClientStatisticsDto> stats = statisticsDao.getClientStatisticsById(testUser1.getId());

        assertThat(stats).isPresent();
        assertThat(stats.get().getTotalVisits()).isEqualTo(1); // Only approved
    }

    @Test
    void shouldOrderStatisticsByTotalSpendingDesc() {
        createReservation(testUser1, LocalDateTime.now().plusDays(1), 60, new BigDecimal("50.00"), ReservationStatus.CONFIRMED_BY_CLIENT);
        createReservation(testUser2, LocalDateTime.now().plusDays(1), 60, new BigDecimal("200.00"), ReservationStatus.CONFIRMED_BY_CLIENT);
        entityManager.flush();

        List<ClientStatisticsDto> statistics = statisticsDao.getClientStatistics();

        assertThat(statistics).hasSize(2);
        assertThat(statistics.get(0).getTotalSpending()).isGreaterThan(statistics.get(1).getTotalSpending());
        assertThat(statistics.get(0).getClientEmail()).isEqualTo("user2@example.com");
    }

    private void createReservation(User user, LocalDateTime startTime, int durationMinutes, ReservationStatus status) {
        createReservation(user, startTime, durationMinutes, new BigDecimal("100.00"), status);
    }

    private void createReservation(User user, LocalDateTime startTime, int durationMinutes,
                                   BigDecimal price, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setEmployee(testEmployee);
        reservation.setStartTime(startTime);
        reservation.setEndTime(startTime.plusMinutes(durationMinutes));
        reservation.setServices(Set.of(testService));
        reservation.setTotalPrice(price);
        reservation.setStatus(status);
        entityManager.persist(reservation);
    }
}
