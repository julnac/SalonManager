package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.salonmanager.salon_manager.model.dto.employee.response.EmployeeDto;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.CreateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.UpdateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.request.CreateEmployeeScheduleRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.response.EmployeeScheduleDto;
import pl.edu.salonmanager.salon_manager.service.EmployeeService;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "Get all employees (PUBLIC)", description = "Returns list of all employees")
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        log.info("REST request to get all employees");
        List<EmployeeDto> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID (PUBLIC)", description = "Returns a single employee by ID")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        log.info("REST request to get employee: {}", id);
        EmployeeDto employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @PostMapping
    @Operation(summary = "Create new employee (ADMIN)", description = "Creates a new employee")
    public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        log.info("REST request to create employee: {} {}", request.getFirstName(), request.getLastName());
        EmployeeDto created = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee (ADMIN)", description = "Updates an existing employee")
    public ResponseEntity<EmployeeDto> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {

        log.info("REST request to update employee: {}", id);
        EmployeeDto updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee (ADMIN)", description = "Deletes an employee")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        log.info("REST request to delete employee: {}", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Get employee schedule (ADMIN)", description = "Returns weekly schedule for a specific employee")
    public ResponseEntity<List<EmployeeScheduleDto>> getEmployeeSchedule(@PathVariable Long id) {
        log.info("REST request to get schedule for employee: {}", id);
        List<EmployeeScheduleDto> schedule = employeeService.getEmployeeSchedule(id);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<List<EmployeeScheduleDto>> createSchedule(
            @PathVariable Long id,
            @RequestBody @Valid List<CreateEmployeeScheduleRequest> request
    ) {
        return ResponseEntity.ok(
                employeeService.createEmployeeSchedule(request, id)
        );
    }

    @PutMapping("/{id}/schedule")
    public ResponseEntity<List<EmployeeScheduleDto>> updateSchedule(
            @PathVariable Long id,
            @RequestBody @Valid List<CreateEmployeeScheduleRequest> request
    ) {
        return ResponseEntity.ok(
                employeeService.updateEmployeeSchedule(request, id)
        );
    }

    @DeleteMapping("/{employeeId}/schedule/{dayOfWeek}")
    @Operation(summary = "Delete employees schedule (ADMIN)", description = "Deletes an employee schedule")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long employeeId,
            @PathVariable DayOfWeek dayOfWeek
    ) {
        employeeService.deleteEmployeeSchedule(employeeId, dayOfWeek);
        return ResponseEntity.noContent().build();
    }
}
