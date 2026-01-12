package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pl.edu.salonmanager.salon_manager.model.dto.user.request.UserRegistrationDto;
import pl.edu.salonmanager.salon_manager.model.dto.user.response.UserDto;
import pl.edu.salonmanager.salon_manager.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users (ADMIN)", description = "Returns list of all users with their roles")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("REST request to get all users");
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user (PUBLIC)", description = "Registers a new user with validation")
    public String register(@Valid @RequestBody UserRegistrationDto dto) {
        // Business logic is executed only if validation passes
        return "Registration successful";
    }
}
