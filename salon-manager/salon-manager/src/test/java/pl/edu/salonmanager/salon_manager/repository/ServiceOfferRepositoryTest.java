package pl.edu.salonmanager.salon_manager.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceOfferRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ServiceOfferRepository serviceOfferRepository;

    @Test
    void shouldSaveServiceOffer() {
        ServiceOffer service = new ServiceOffer();
        service.setName("Haircut");
        service.setPrice(new BigDecimal("50.00"));
        service.setDurationMinutes(30);

        ServiceOffer saved = serviceOfferRepository.save(service);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Haircut");
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldFindServiceById() {
        ServiceOffer service = new ServiceOffer();
        service.setName("Manicure");
        service.setPrice(new BigDecimal("30.00"));
        service.setDurationMinutes(45);
        entityManager.persist(service);
        entityManager.flush();

        Optional<ServiceOffer> found = serviceOfferRepository.findById(service.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Manicure");
    }

    @Test
    void shouldFindAllServices() {
        ServiceOffer service1 = createService("Service 1", "25.00", 20);
        ServiceOffer service2 = createService("Service 2", "35.00", 30);
        entityManager.persist(service1);
        entityManager.persist(service2);
        entityManager.flush();

        List<ServiceOffer> all = serviceOfferRepository.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldUpdateService() {
        ServiceOffer service = createService("Old Name", "50.00", 30);
        entityManager.persist(service);
        entityManager.flush();
        entityManager.clear();

        ServiceOffer found = serviceOfferRepository.findById(service.getId()).orElseThrow();
        found.setName("New Name");
        found.setPrice(new BigDecimal("60.00"));
        serviceOfferRepository.save(found);
        entityManager.flush();
        entityManager.clear();

        ServiceOffer updated = serviceOfferRepository.findById(service.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void shouldDeleteService() {
        ServiceOffer service = createService("To Delete", "100.00", 60);
        entityManager.persist(service);
        entityManager.flush();
        Long serviceId = service.getId();

        serviceOfferRepository.deleteById(serviceId);
        entityManager.flush();

        assertThat(serviceOfferRepository.findById(serviceId)).isEmpty();
    }

    private ServiceOffer createService(String name, String price, int duration) {
        ServiceOffer service = new ServiceOffer();
        service.setName(name);
        service.setPrice(new BigDecimal(price));
        service.setDurationMinutes(duration);
        return service;
    }
}
