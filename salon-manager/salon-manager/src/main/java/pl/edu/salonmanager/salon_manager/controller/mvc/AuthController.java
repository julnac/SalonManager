package pl.edu.salonmanager.salon_manager.controller.mvc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.edu.salonmanager.salon_manager.model.entity.Role;
import pl.edu.salonmanager.salon_manager.model.entity.User;
import pl.edu.salonmanager.salon_manager.repository.RoleRepository;
import pl.edu.salonmanager.salon_manager.repository.UserRepository;

import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Nieprawidłowy email lub hasło");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Zostałeś wylogowany");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {

        log.debug("Processing registration for: {}", form.getEmail());

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Hasła nie są identyczne");
            return "auth/register";
        }

        if (userRepository.existsByEmail(form.getEmail())) {
            model.addAttribute("errorMessage", "Użytkownik z tym adresem email już istnieje");
            return "auth/register";
        }

        try {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found. Please run data initialization."));

            User user = User.builder()
                    .email(form.getEmail())
                    .password(passwordEncoder.encode(form.getPassword()))
                    .firstName(form.getFirstName())
                    .lastName(form.getLastName())
                    .enabled(true)
                    .roles(Set.of(userRole))
                    .build();

            userRepository.save(user);

            log.info("User registered successfully: {}", user.getEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rejestracja zakończona sukcesem! Możesz się teraz zalogować.");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("Error during registration: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Wystąpił błąd podczas rejestracji");
            return "auth/register";
        }
    }

    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication) {
        log.debug("Login successful for user: {}", authentication.getName());

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            log.debug("User has role: {}", role);

            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (role.equals("ROLE_USER")) {
                return "redirect:/client/dashboard";
            }
        }

        return "redirect:/";
    }

    @Data
    public static class RegisterForm {
        @NotBlank(message = "Imię jest wymagane")
        @Size(min = 2, max = 50, message = "Imię musi mieć od 2 do 50 znaków")
        private String firstName;

        @NotBlank(message = "Nazwisko jest wymagane")
        @Size(min = 2, max = 50, message = "Nazwisko musi mieć od 2 do 50 znaków")
        private String lastName;

        @NotBlank(message = "Email jest wymagany")
        @Email(message = "Podaj prawidłowy adres email")
        private String email;

        @NotBlank(message = "Hasło jest wymagane")
        @Size(min = 6, message = "Hasło musi mieć co najmniej 6 znaków")
        private String password;

        @NotBlank(message = "Potwierdzenie hasła jest wymagane")
        private String confirmPassword;
    }
}
