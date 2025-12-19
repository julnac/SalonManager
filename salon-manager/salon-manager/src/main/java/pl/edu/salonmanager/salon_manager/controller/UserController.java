package pl.edu.salonmanager.salon_manager.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pl.edu.salonmanager.salon_manager.model.dto.request.UserRegistrationDto;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/register")
    public String register(@Valid @RequestBody UserRegistrationDto dto) {
        // Business logic is executed only if validation passes
        return "Registration successful";
    }
}
