package pl.edu.salonmanager.salon_manager.controller.mvc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.exception.UnauthorizedException;
import pl.edu.salonmanager.salon_manager.model.dto.reservation.request.CreateReservationRequest;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.EmployeeService;
import pl.edu.salonmanager.salon_manager.service.ReservationService;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

import java.time.LocalDateTime;
import java.util.Set;

@Controller
@RequestMapping("/client/reservations")
@RequiredArgsConstructor
@Slf4j
public class ClientReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    @GetMapping
    public String myReservations(Model model) {
        Long userId = getCurrentUserId();
        model.addAttribute("reservations", reservationService.getMyReservations(userId));
        return "client/reservations/list";
    }

    @PostMapping("/{id}/cancel")
    public String cancelReservation(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            reservationService.cancelReservation(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Rezerwacja anulowana");
            return "redirect:/client/reservations";
        } catch (Exception e) {
            log.error("Error cancelling reservation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/client/reservations";
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirmReservation(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            reservationService.confirmReservation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Rezerwacja potwierdzona");
            return "redirect:/client/reservations";
        } catch (Exception e) {
            log.error("Error confirming reservation: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/client/reservations";
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return user.getId();
    }

}
