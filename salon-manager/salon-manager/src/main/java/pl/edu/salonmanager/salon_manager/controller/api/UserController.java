package pl.edu.salonmanager.salon_manager.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegistrationDto dto) {
        log.info("REST request to register new user: {}", dto.getEmail());
        UserDto registeredUser = userService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }
}
