package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.response.EmployeeSpecializationDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateEmployeeSpecializationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSpecialization;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;
import pl.edu.salonmanager.salon_manager.repository.EmployeeSpecializationRepository;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeSpecializationService {

    private final EmployeeSpecializationRepository employeeSpecializationRepository;
    private final EmployeeRepository employeeRepository;
    private final ServiceOfferRepository serviceOfferRepository;

    @Transactional(readOnly = true)
    public List<EmployeeSpecializationDto> getAllSpecializations() {
        log.debug("Fetching all employee specializations");
        return employeeSpecializationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeSpecializationDto getSpecializationById(Long id) {
        log.debug("Fetching specialization with id: {}", id);
        EmployeeSpecialization specialization = employeeSpecializationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with id: " + id));
        return mapToDto(specialization);
    }

    @Transactional
    public EmployeeSpecializationDto createSpecialization(CreateEmployeeSpecializationRequest request) {
        log.debug("Creating specialization for employee {} and service {}",
                request.getEmployeeId(), request.getServiceId());

        Employee employee = employeeRepository.findById(request.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        ServiceOffer service = serviceOfferRepository.findById(request.getServiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + request.getServiceId()));

        EmployeeSpecialization specialization = new EmployeeSpecialization();
        specialization.setEmployee(employee);
        specialization.setServiceOffer(service);
        specialization.setExperienceYears(request.getExperienceYears());

        EmployeeSpecialization saved = employeeSpecializationRepository.save(specialization);
        log.info("Specialization created successfully with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public void deleteSpecialization(Long id) {
        log.debug("Deleting specialization with id: {}", id);

        if (!employeeSpecializationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Specialization not found with id: " + id);
        }

        employeeSpecializationRepository.deleteById(id);
        log.info("Specialization deleted successfully with id: {}", id);
    }

    // Mapper: Entity → DTO
    private EmployeeSpecializationDto mapToDto(EmployeeSpecialization entity) {
        return new EmployeeSpecializationDto(
                entity.getId(),
                entity.getEmployee().getId(),
                entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName(),
                entity.getServiceOffer().getId(),
                entity.getServiceOffer().getName(),
                entity.getExperienceYears()
        );
    }
}
