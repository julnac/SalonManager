package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.model.dto.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.request.UpdateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceOfferService {

    private final ServiceOfferRepository serviceOfferRepository;

    @Transactional(readOnly = true)
    public List<ServiceOfferDto> getAllServices() {
        log.debug("Fetching all services");
        return serviceOfferRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceOfferDto getServiceById(Long id) {
        log.debug("Fetching service with id: {}", id);
        ServiceOffer serviceOffer = serviceOfferRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Service not found with id: {}", id);
                return new RuntimeException("Service not found with id: " + id);
            });
        return mapToDto(serviceOffer);
    }

    @Transactional
    public ServiceOfferDto createService(CreateServiceRequest request) {
        log.info("Creating new service: {}", request.getName());

        ServiceOffer serviceOffer = new ServiceOffer();
        serviceOffer.setName(request.getName());
        serviceOffer.setPrice(request.getPrice());
        serviceOffer.setDurationMinutes(request.getDurationMinutes());

        ServiceOffer saved = serviceOfferRepository.save(serviceOffer);
        log.info("Service created with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public ServiceOfferDto updateService(Long id, UpdateServiceRequest request) {
        log.info("Updating service with id: {}", id);

        ServiceOffer existing = serviceOfferRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Service not found with id: {}", id);
                return new RuntimeException("Service not found with id: " + id);
            });

        existing.setName(request.getName());
        existing.setPrice(request.getPrice());
        existing.setDurationMinutes(request.getDurationMinutes());

        ServiceOffer updated = serviceOfferRepository.save(existing);
        log.info("Service updated with id: {}", updated.getId());
        return mapToDto(updated);
    }

    @Transactional
    public void deleteService(Long id) {
        log.info("Deleting service with id: {}", id);

        if (!serviceOfferRepository.existsById(id)) {
            log.error("Service not found with id: {}", id);
            throw new RuntimeException("Service not found with id: " + id);
        }

        serviceOfferRepository.deleteById(id);
        log.info("Service deleted with id: {}", id);
    }

    // Mapper: Entity → DTO
    private ServiceOfferDto mapToDto(ServiceOffer entity) {
        return new ServiceOfferDto(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getDurationMinutes()
        );
    }
}
