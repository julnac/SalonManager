package pl.edu.salonmanager.salon_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;

import java.util.List;

@Repository
public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {

    List<ServiceOffer> findByNameContaining(String name);

    List<ServiceOffer> findByNameContainingIgnoreCase(String name);
}
