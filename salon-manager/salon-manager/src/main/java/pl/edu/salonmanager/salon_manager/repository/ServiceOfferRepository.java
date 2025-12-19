package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {

    List<ServiceOffer> findByNameContaining(String name);

    List<ServiceOffer> findByNameContainingIgnoreCase(String name);

    // @Query przykład - Native SQL z agregacją i statystykami
    @Query(value = "SELECT so.id, so.name, so.price, so.duration_minutes, " +
           "COUNT(rs.reservation_id) as booking_count, SUM(so.price) as total_revenue " +
           "FROM service_offers so " +
           "LEFT JOIN reservation_services rs ON so.id = rs.service_offer_id " +
           "LEFT JOIN reservations r ON rs.reservation_id = r.id " +
           "WHERE r.status IN ('APPROVED', 'CONFIRMED_BY_CLIENT') OR r.id IS NULL " +
           "GROUP BY so.id, so.name, so.price, so.duration_minutes " +
           "ORDER BY booking_count DESC", nativeQuery = true)
    List<Object[]> findPopularServicesWithStats();

    // @Query przykład - JPQL z zakresem cenowym
    @Query("SELECT s FROM ServiceOffer s " +
           "WHERE s.price BETWEEN :minPrice AND :maxPrice ORDER BY s.price ASC")
    Page<ServiceOffer> findByPriceRange(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    // @Query przykład - usługi pracownika
    @Query("SELECT DISTINCT s FROM ServiceOffer s " +
           "JOIN s.employeeSpecializations es WHERE es.employee.id = :employeeId ORDER BY s.name")
    List<ServiceOffer> findByEmployeeId(@Param("employeeId") Long employeeId);

    // Paginacja
    Page<ServiceOffer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
