package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.response.AvailabilityResponseDto;
import pl.edu.salonmanager.salon_manager.model.dto.response.EmployeeAvailabilityDto;
import pl.edu.salonmanager.salon_manager.model.entity.Employee;
import pl.edu.salonmanager.salon_manager.model.entity.EmployeeSchedule;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.EmployeeScheduleRepository;
import pl.edu.salonmanager.salon_manager.repository.EmployeeSpecializationRepository;
import pl.edu.salonmanager.salon_manager.repository.ReservationRepository;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final ServiceOfferRepository serviceOfferRepository;
    private final EmployeeSpecializationRepository employeeSpecializationRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final SalonProperties salonProperties;

    @Transactional(readOnly = true)
    public AvailabilityResponseDto findAvailableSlots(LocalDate date, List<Long> serviceIds) {
        log.debug("Finding available slots for date: {} and services: {}", date, serviceIds);

        validateSearchRequest(date, serviceIds);

        int totalDurationMinutes = calculateTotalDuration(serviceIds);
        log.debug("Total duration needed: {} minutes", totalDurationMinutes);

        List<Employee> qualifiedEmployees = findQualifiedEmployees(serviceIds);
        log.debug("Found {} qualified employees", qualifiedEmployees.size());

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<EmployeeAvailabilityDto> employeeAvailabilities = new ArrayList<>();

        for (Employee employee : qualifiedEmployees) {
            EmployeeAvailabilityDto availability = findEmployeeAvailableSlots(
                employee, date, dayOfWeek, totalDurationMinutes
            );

            if (availability != null && !availability.getAvailableSlots().isEmpty()) {
                employeeAvailabilities.add(availability);
            }
        }

        log.info("Found availability for {} employees on {}", employeeAvailabilities.size(), date);

        return new AvailabilityResponseDto(date, totalDurationMinutes, employeeAvailabilities);
    }

    private void validateSearchRequest(LocalDate date, List<Long> serviceIds) {
        if (date == null) {
            throw new BadRequestException("Date cannot be null");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot search for dates in the past");
        }

        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new BadRequestException("Service IDs list cannot be empty");
        }

        List<ServiceOffer> services = serviceOfferRepository.findAllById(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new ResourceNotFoundException("One or more services not found");
        }
    }


    private int calculateTotalDuration(List<Long> serviceIds) {
        List<ServiceOffer> services = serviceOfferRepository.findAllById(serviceIds);
        return services.stream()
            .mapToInt(ServiceOffer::getDurationMinutes)
            .sum();
    }

    private List<Employee> findQualifiedEmployees(List<Long> serviceIds) {
        return employeeSpecializationRepository.findEmployeesWithAllServices(
            serviceIds,
            (long) serviceIds.size()
        );
    }

    private EmployeeAvailabilityDto findEmployeeAvailableSlots(
            Employee employee,
            LocalDate date,
            DayOfWeek dayOfWeek,
            int totalDurationMinutes) {

        log.debug("Checking availability for employee: {} {} on {}",
            employee.getFirstName(), employee.getLastName(), date);

        EmployeeSchedule schedule = employeeScheduleRepository
            .findByEmployeeIdAndDayOfWeek(employee.getId(), dayOfWeek)
            .orElse(null);

        if (schedule == null || !schedule.getIsWorkingDay()) {
            log.debug("Employee {} does not work on {}", employee.getId(), dayOfWeek);
            return null;
        }

        List<Reservation> reservations = reservationRepository
            .findActiveReservationsByEmployeeAndDate(employee.getId(), date);

        log.debug("Employee has {} existing reservations on {}", reservations.size(), date);

        List<LocalTime> availableSlots = generateAvailableSlots(
            schedule.getStartTime(),
            schedule.getEndTime(),
            totalDurationMinutes,
            reservations,
            date
        );

        if (availableSlots.isEmpty()) {
            log.debug("No available slots found for employee {}", employee.getId());
            return null;
        }

        log.debug("Found {} available slots for employee {}", availableSlots.size(), employee.getId());

        return new EmployeeAvailabilityDto(
            employee.getId(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getEmail(),
            availableSlots
        );
    }

    private List<LocalTime> generateAvailableSlots(
            LocalTime workStart,
            LocalTime workEnd,
            int totalDurationMinutes,
            List<Reservation> reservations,
            LocalDate date) {

        List<LocalTime> availableSlots = new ArrayList<>();
        int slotDuration = salonProperties.getSlotDurationMinutes();

        int slotsNeeded = (int) Math.ceil((double) totalDurationMinutes / slotDuration);
        int totalMinutesNeeded = slotsNeeded * slotDuration;

        log.debug("Need {} slots ({} minutes) for total duration of {} minutes",
            slotsNeeded, totalMinutesNeeded, totalDurationMinutes);

        LocalTime currentSlot = workStart;

        while (currentSlot.plusMinutes(totalMinutesNeeded).isBefore(workEnd)
               || currentSlot.plusMinutes(totalMinutesNeeded).equals(workEnd)) {

            LocalTime slotEndTime = currentSlot.plusMinutes(totalMinutesNeeded);

            if (isSlotFree(currentSlot, slotEndTime, reservations, date)) {
                availableSlots.add(currentSlot);
            }

            currentSlot = currentSlot.plusMinutes(slotDuration);
        }

        return availableSlots;
    }

    private boolean isSlotFree(
            LocalTime slotStart,
            LocalTime slotEnd,
            List<Reservation> reservations,
            LocalDate date) {

        LocalDateTime requestedStart = LocalDateTime.of(date, slotStart);
        LocalDateTime requestedEnd = LocalDateTime.of(date, slotEnd);

        for (Reservation reservation : reservations) {
            if (timesOverlap(requestedStart, requestedEnd, reservation.getStartTime(), reservation.getEndTime())) {
                return false;
            }
        }

        return true;
    }

    private boolean timesOverlap(
            LocalDateTime start1,
            LocalDateTime end1,
            LocalDateTime start2,
            LocalDateTime end2) {

        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
