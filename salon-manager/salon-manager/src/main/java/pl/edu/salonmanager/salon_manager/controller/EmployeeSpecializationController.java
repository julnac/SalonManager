package pl.edu.salonmanager.salon_manager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.model.dto.response.EmployeeSpecializationDto;
import pl.edu.salonmanager.salon_manager.model.dto.request.CreateEmployeeSpecializationRequest;
import pl.edu.salonmanager.salon_manager.service.EmployeeSpecializationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-specializations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee Specializations", description = "Employee specialization management endpoints")
public class EmployeeSpecializationController {

    private final EmployeeSpecializationService employeeSpecializationService;

    @GetMapping
    @Operation(summary = "Get all specializations", description = "Returns list of all employee specializations")
    public ResponseEntity<List<EmployeeSpecializationDto>> getAllSpecializations() {
        log.info("REST request to get all specializations");
        List<EmployeeSpecializationDto> specializations = employeeSpecializationService.getAllSpecializations();
        return ResponseEntity.ok(specializations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specialization by ID", description = "Returns a single specialization by ID")
    public ResponseEntity<EmployeeSpecializationDto> getSpecializationById(@PathVariable Long id) {
        log.info("REST request to get specialization: {}", id);
        EmployeeSpecializationDto specialization = employeeSpecializationService.getSpecializationById(id);
        return ResponseEntity.ok(specialization);
    }

    @PostMapping
    @Operation(summary = "Create specialization", description = "Creates a new employee specialization")
    public ResponseEntity<EmployeeSpecializationDto> createSpecialization(
            @Valid @RequestBody CreateEmployeeSpecializationRequest request) {
        log.info("REST request to create specialization");
        EmployeeSpecializationDto created = employeeSpecializationService.createSpecialization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete specialization", description = "Deletes an employee specialization")
    public ResponseEntity<Void> deleteSpecialization(@PathVariable Long id) {
        log.info("REST request to delete specialization: {}", id);
        employeeSpecializationService.deleteSpecialization(id);
        return ResponseEntity.noContent().build();
    }
}
