package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.response.EmployeeDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.request.UpdateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<EmployeeDto> getAllEmployees() {
        log.debug("Fetching all employees");
        return employeeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(Long id) {
        log.debug("Fetching employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return mapToDto(employee);
    }

    @Transactional
    public EmployeeDto createEmployee(CreateEmployeeRequest request) {
        log.debug("Creating new employee: {} {}", request.getFirstName(), request.getLastName());

        // Check if email already exists
        if (employeeRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to create employee with duplicate email: {}", request.getEmail());
            throw new BadRequestException("Employee with email " + request.getEmail() + " already exists");
        }

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.debug("Updating employee with id: {}", id);

        Employee existing = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        // Check if email is being changed and if new email already exists
        if (!existing.getEmail().equals(request.getEmail()) &&
            employeeRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempt to update employee {} with duplicate email: {}", id, request.getEmail());
            throw new BadRequestException("Employee with email " + request.getEmail() + " already exists");
        }

        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setEmail(request.getEmail());

        Employee updated = employeeRepository.save(existing);
        log.info("Employee updated successfully with id: {}", updated.getId());
        return mapToDto(updated);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        log.debug("Deleting employee with id: {}", id);

        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }

        employeeRepository.deleteById(id);
        log.info("Employee deleted successfully with id: {}", id);
    }

    // Mapper: Entity → DTO
    private EmployeeDto mapToDto(Employee entity) {
        return new EmployeeDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail()
        );
    }
}
