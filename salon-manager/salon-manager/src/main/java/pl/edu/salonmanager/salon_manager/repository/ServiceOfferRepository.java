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

    @Query("SELECT s FROM ServiceOffer s " +
           "WHERE s.price BETWEEN :minPrice AND :maxPrice ORDER BY s.price ASC")
    Page<ServiceOffer> findByPriceRange(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    @Query("SELECT DISTINCT s FROM ServiceOffer s " +
           "JOIN s.employeeSpecializations es WHERE es.employee.id = :employeeId ORDER BY s.name")
    List<ServiceOffer> findByEmployeeId(@Param("employeeId") Long employeeId);

    Page<ServiceOffer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
