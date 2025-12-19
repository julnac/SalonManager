package pl.edu.salonmanager.salon_manager.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }

        // Logic: At least one uppercase letter and one special character
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        return hasUppercase && hasSpecialChar;
    }
}
