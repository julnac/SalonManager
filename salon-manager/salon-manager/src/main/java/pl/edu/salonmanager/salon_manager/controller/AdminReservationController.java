package pl.edu.salonmanager.salon_manager.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.dto.request.UpdateReservationAdminRequest;
import pl.edu.salonmanager.salon_manager.model.entity.Reservation;
import pl.edu.salonmanager.salon_manager.model.enums.ReservationStatus;
import pl.edu.salonmanager.salon_manager.service.EmployeeService;
import pl.edu.salonmanager.salon_manager.service.ReservationService;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

import java.util.List;

@Controller
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
@Slf4j
public class AdminReservationController {

    private final ReservationService reservationService;
    private final EmployeeService employeeService;
    private final ServiceOfferService serviceOfferService;

    @GetMapping
    public String listReservations(
            @RequestParam(required = false) ReservationStatus status,
            Model model) {

        log.debug("Listing reservations with status filter: {}", status);

        List<Reservation> reservations = reservationService.getAllReservations(status);

        model.addAttribute("reservations", reservations);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", ReservationStatus.values());

        return "admin/reservations/list";
    }

    @GetMapping("/{id}")
    public String viewReservation(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Viewing reservation: {}", id);

        try {
            Reservation reservation = reservationService.getReservationById(id);
            model.addAttribute("reservation", reservation);
            return "admin/reservations/detail";
        } catch (ResourceNotFoundException e) {
            log.error("Reservation not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
            return "redirect:/admin/reservations";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing edit form for reservation: {}", id);

        try {
            Reservation reservation = reservationService.getReservationById(id);

            UpdateReservationAdminRequest dto = new UpdateReservationAdminRequest();
            dto.setStartTime(reservation.getStartTime());
            dto.setEndTime(reservation.getEndTime());
            dto.setEmployeeId(reservation.getEmployee().getId());
            dto.setServiceIds(reservation.getServices().stream()
                .map(s -> s.getId())
                .collect(java.util.stream.Collectors.toSet()));

            model.addAttribute("reservation", reservation);
            model.addAttribute("reservationForm", dto);
            model.addAttribute("employees", employeeService.getAllEmployees());
            model.addAttribute("services", serviceOfferService.getAllServices());

            return "admin/reservations/edit";
        } catch (ResourceNotFoundException e) {
            log.error("Reservation not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
            return "redirect:/admin/reservations";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateReservation(
            @PathVariable Long id,
            @Valid @ModelAttribute("reservationForm") UpdateReservationAdminRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating reservation: {}", id);

        if (bindingResult.hasErrors()) {
            try {
                Reservation reservation = reservationService.getReservationById(id);
                model.addAttribute("reservation", reservation);
                model.addAttribute("employees", employeeService.getAllEmployees());
                model.addAttribute("services", serviceOfferService.getAllServices());
                return "admin/reservations/edit";
            } catch (ResourceNotFoundException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
                return "redirect:/admin/reservations";
            }
        }

        try {
            reservationService.updateReservation(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Rezerwacja została zaktualizowana");
            return "redirect:/admin/reservations/" + id;
        } catch (BadRequestException e) {
            log.error("Error updating reservation: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());

            try {
                Reservation reservation = reservationService.getReservationById(id);
                model.addAttribute("reservation", reservation);
                model.addAttribute("employees", employeeService.getAllEmployees());
                model.addAttribute("services", serviceOfferService.getAllServices());
                return "admin/reservations/edit";
            } catch (ResourceNotFoundException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
                return "redirect:/admin/reservations";
            }
        } catch (ResourceNotFoundException e) {
            log.error("Reservation not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
            return "redirect:/admin/reservations";
        }
    }

    @PostMapping("/{id}/approve")
    public String approveReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Approving reservation: {}", id);

        try {
            reservationService.approveReservation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Rezerwacja została zatwierdzona");
        } catch (BadRequestException e) {
            log.error("Cannot approve reservation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.error("Reservation not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
        }

        return "redirect:/admin/reservations/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.debug("Cancelling reservation: {}", id);

        try {
            reservationService.cancelReservation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Rezerwacja została anulowana");
        } catch (BadRequestException e) {
            log.error("Cannot cancel reservation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.error("Reservation not found: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Rezerwacja nie została znaleziona");
        }

        return "redirect:/admin/reservations/" + id;
    }
}
