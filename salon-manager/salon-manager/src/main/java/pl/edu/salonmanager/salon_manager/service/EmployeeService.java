package pl.edu.salonmanager.salon_manager.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.employee.response.EmployeeDto;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.CreateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employee.request.UpdateEmployeeRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.request.CreateEmployeeScheduleRequest;
import pl.edu.salonmanager.salon_manager.model.dto.employeeSchedule.response.EmployeeScheduleDto;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSchedule;
import pl.edu.salonmanager.salon_manager.repository.EmployeeRepository;
import pl.edu.salonmanager.salon_manager.repository.EmployeeScheduleRepository;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;

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

    @Transactional(readOnly = true)
    public List<EmployeeScheduleDto> getEmployeeSchedule(Long employeeId) {
        log.debug("Fetching schedule for employee with id: {}", employeeId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }

        List<EmployeeSchedule> schedules = employeeScheduleRepository.findByEmployeeId(employeeId);
        return schedules.stream()
                .map(this::mapScheduleToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<EmployeeScheduleDto> createEmployeeSchedule(@Valid List<CreateEmployeeScheduleRequest> request, Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> {
                log.warn("Attempt to create schedule for not existing employee");
                return new BadRequestException("No such employee exists");
            });

        log.debug("Creating new chedule for employee {}", employee);


        List<EmployeeSchedule> schedules = new ArrayList<>();

        for (CreateEmployeeScheduleRequest req : request) {

            if (employeeScheduleRepository
                    .existsByEmployeeAndDayOfWeek(employee, req.getDayOfWeek())) {
                throw new BadRequestException(
                        "Schedule for " + req.getDayOfWeek() + " already exists"
                );
            }

            EmployeeSchedule schedule = new EmployeeSchedule();
            schedule.setEmployee(employee);
            schedule.setDayOfWeek(req.getDayOfWeek());
            schedule.setStartTime(req.getStartTime());
            schedule.setEndTime(req.getEndTime());
            schedule.setIsWorkingDay(req.getStartTime() != null && req.getEndTime() != null);

            schedules.add(schedule);
        }

        List<EmployeeSchedule> saved = employeeScheduleRepository.saveAll(schedules);

        log.info("Schedule created successfully for employee: {}", employee.getId());
        return saved.stream()
                .map(this::mapScheduleToDto)
                .toList();
    }

    @Transactional
    public void deleteEmployeeSchedule( Long employeeId, DayOfWeek dayOfWeek) {

        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> {
                log.warn("Attempt to delete schedule for not existing employee");
                return new BadRequestException("No such employee exists");
            });

        EmployeeSchedule schedule = employeeScheduleRepository.findByEmployeeAndDayOfWeek( employee, dayOfWeek)
            .orElseThrow(() -> {
                log.warn("Attempt to delete not existing schedule");
                return new BadRequestException("No such employee exists");
            });

        employeeScheduleRepository.deleteById(schedule.getId());
        log.info("Schedule deleted successfully with id");
    }

    @Transactional
    public List<EmployeeScheduleDto> updateEmployeeSchedule(@Valid List<CreateEmployeeScheduleRequest> request, Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> {
                log.warn("Attempt to update schedule for not existing employee");
                return new BadRequestException("No such employee exists");
            });

        log.debug("Updating schedule for employee {}", employee);

        List<EmployeeSchedule> updatedSchedules = new ArrayList<>();

        for (CreateEmployeeScheduleRequest req : request) {

            EmployeeSchedule schedule = employeeScheduleRepository
                    .findByEmployeeAndDayOfWeek(employee, req.getDayOfWeek())
                    .orElseGet(() -> {
                        EmployeeSchedule newSchedule = new EmployeeSchedule();
                        newSchedule.setEmployee(employee);
                        newSchedule.setDayOfWeek(req.getDayOfWeek());
                        return newSchedule;
                    });

            schedule.setStartTime(req.getStartTime());
            schedule.setEndTime(req.getEndTime());
            schedule.setIsWorkingDay(req.getStartTime() != null && req.getEndTime() != null);

            updatedSchedules.add(schedule);
        }

        List<EmployeeSchedule> saved = employeeScheduleRepository.saveAll(updatedSchedules);

        log.info("Schedule updated successfully for employee: {}", employee.getId());
        return saved.stream()
                .map(this::mapScheduleToDto)
                .toList();
    }

    private EmployeeDto mapToDto(Employee entity) {
        return new EmployeeDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail()
        );
    }

    private EmployeeScheduleDto mapScheduleToDto(EmployeeSchedule schedule) {
        return new EmployeeScheduleDto(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getIsWorkingDay()
        );
    }
}
