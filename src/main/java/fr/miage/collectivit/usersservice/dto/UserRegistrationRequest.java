package fr.miage.collectivit.usersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.ToString;

@Builder
public record UserRegistrationRequest(
    @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
    @NotBlank(message = "Last name is mandatory")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
    @NotNull(message = "Birth date is mandatory") @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
    @NotBlank(message = "Email is mandatory")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
    @NotBlank(message = "Password is mandatory")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=]).+$",
            message =
                "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        @ToString.Exclude
        String password,
    List<String> licensePlates,
    // Physical card pickup location is optional as the customer can choose not to request a
    // physical card
    @Size(max = 150, message = "Pickup location must not exceed 150 characters")
        String pickupLocation) {}
