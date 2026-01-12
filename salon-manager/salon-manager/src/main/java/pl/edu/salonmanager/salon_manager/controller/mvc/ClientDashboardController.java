package pl.edu.salonmanager.salon_manager.controller.mvc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.edu.salonmanager.salon_manager.exception.ResourceNotFoundException;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;
import pl.edu.salonmanager.salon_manager.service.ReservationService;

@Controller
@RequestMapping("/client/dashboard")
@RequiredArgsConstructor
@Slf4j
public class ClientDashboardController {

    private final UserRepository userRepository;
    private final ReservationService reservationService;

    @GetMapping
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        model.addAttribute("user", user);
        
        return "client/dashboard";
    }
}
