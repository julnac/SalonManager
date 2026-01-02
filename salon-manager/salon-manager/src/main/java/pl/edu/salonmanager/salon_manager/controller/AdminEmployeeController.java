package pl.edu.salonmanager.salon_manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.service.EmployeeService;
import pl.edu.salonmanager.salon_manager.service.ReservationService;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
@Slf4j
public class AdminEmployeeController {

    private final EmployeeService employeeService;
    private final ReservationService reservationService;

    @GetMapping
    public String listEmployees(Model model) {
        log.debug("Listing all employees");
        model.addAttribute("employees", employeeService.getAllEmployees());
        return "admin/employees/list";
    }

    @GetMapping("/{id}/schedule")
    public String viewSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Viewing schedule for employee: {}", id);

        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = startDate.plusDays(7);
            }

            model.addAttribute("employee", employeeService.getEmployeeById(id));
            model.addAttribute("reservations", reservationService.getReservationsByEmployeeAndDateRange(id, startDate, endDate));
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "admin/employees/schedule";
        } catch (ResourceNotFoundException e) {
            log.error("Employee not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Pracownik nie został znaleziony");
            return "redirect:/admin/employees";
        }
    }
}
