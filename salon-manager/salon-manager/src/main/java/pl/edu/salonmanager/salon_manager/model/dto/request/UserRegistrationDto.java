package pl.edu.salonmanager.salon_manager.model.dto.request;
import jakarta.validation.constraints.*;
import pl.edu.salonmanager.salon_manager.validation.StrongPassword;

public class UserRegistrationDto {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @StrongPassword
    private String password;

    // Getters and Setters
}
