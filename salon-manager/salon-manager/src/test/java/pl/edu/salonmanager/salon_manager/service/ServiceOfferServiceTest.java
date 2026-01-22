package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.UpdateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOfferServiceTest {

    @Mock
    private ServiceOfferRepository serviceOfferRepository;

    @InjectMocks
    private ServiceOfferService serviceOfferService;

    private ServiceOffer service1;
    private ServiceOffer service2;
    private CreateServiceRequest createRequest;
    private UpdateServiceRequest updateRequest;

    @BeforeEach
    void setUp() {
        service1 = new ServiceOffer();
        service1.setId(1L);
        service1.setName("Haircut");
        service1.setPrice(new BigDecimal("50.00"));
        service1.setDurationMinutes(30);

        service2 = new ServiceOffer();
        service2.setId(2L);
        service2.setName("Hair Coloring");
        service2.setPrice(new BigDecimal("100.00"));
        service2.setDurationMinutes(60);

        createRequest = new CreateServiceRequest();
        createRequest.setName("Manicure");
        createRequest.setPrice(new BigDecimal("40.00"));
        createRequest.setDurationMinutes(45);

        updateRequest = new UpdateServiceRequest();
        updateRequest.setName("Updated Haircut");
        updateRequest.setPrice(new BigDecimal("60.00"));
        updateRequest.setDurationMinutes(40);
    }

    // ========== getAllServices Tests ==========

    @Test
    void shouldGetAllServices() {
        // Given
        when(serviceOfferRepository.findAll()).thenReturn(Arrays.asList(service1, service2));

        // When
        List<ServiceOfferDto> result = serviceOfferService.getAllServices();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Haircut");
        assertThat(result.get(1).getName()).isEqualTo("Hair Coloring");
        verify(serviceOfferRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoServices() {
        // Given
        when(serviceOfferRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<ServiceOfferDto> result = serviceOfferService.getAllServices();

        // Then
        assertThat(result).isEmpty();
        verify(serviceOfferRepository).findAll();
    }

    // ========== getServiceById Tests ==========

    @Test
    void shouldGetServiceById() {
        // Given
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(service1));

        // When
        ServiceOfferDto result = serviceOfferService.getServiceById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Haircut");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getDurationMinutes()).isEqualTo(30);
        verify(serviceOfferRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenServiceNotFound() {
        // Given
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> serviceOfferService.getServiceById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
        verify(serviceOfferRepository).findById(1L);
    }

    // ========== createService Tests ==========

    @Test
    void shouldCreateService() {
        // Given
        ServiceOffer savedService = new ServiceOffer();
        savedService.setId(3L);
        savedService.setName("Manicure");
        savedService.setPrice(new BigDecimal("40.00"));
        savedService.setDurationMinutes(45);

        when(serviceOfferRepository.save(any(ServiceOffer.class))).thenReturn(savedService);

        // When
        ServiceOfferDto result = serviceOfferService.createService(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Manicure");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.getDurationMinutes()).isEqualTo(45);

        verify(serviceOfferRepository).save(any(ServiceOffer.class));
    }

    // ========== updateService Tests ==========

    @Test
    void shouldUpdateService() {
        // Given
        ServiceOffer updatedService = new ServiceOffer();
        updatedService.setId(1L);
        updatedService.setName("Updated Haircut");
        updatedService.setPrice(new BigDecimal("60.00"));
        updatedService.setDurationMinutes(40);

        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.of(service1));
        when(serviceOfferRepository.save(any(ServiceOffer.class))).thenReturn(updatedService);

        // When
        ServiceOfferDto result = serviceOfferService.updateService(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Haircut");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(result.getDurationMinutes()).isEqualTo(40);

        verify(serviceOfferRepository).findById(1L);
        verify(serviceOfferRepository).save(any(ServiceOffer.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentService() {
        // Given
        when(serviceOfferRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> serviceOfferService.updateService(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
        verify(serviceOfferRepository).findById(1L);
    }

    // ========== deleteService Tests ==========

    @Test
    void shouldDeleteService() {
        // Given
        when(serviceOfferRepository.existsById(1L)).thenReturn(true);

        // When
        serviceOfferService.deleteService(1L);

        // Then
        verify(serviceOfferRepository).existsById(1L);
        verify(serviceOfferRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentService() {
        // Given
        when(serviceOfferRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> serviceOfferService.deleteService(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found");
        verify(serviceOfferRepository).existsById(1L);
    }
}
