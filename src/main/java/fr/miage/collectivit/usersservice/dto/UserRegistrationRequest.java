package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.FrenchLicensePlate;
import fr.miage.collectivit.usersservice.validation.MinAge;
import fr.miage.collectivit.usersservice.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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
    @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be in the past")
        @MinAge(value = 18, message = "User must be at least 18 years old")
        LocalDate birthDate,
    @NotBlank(message = "Email is mandatory")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
    @NotBlank(message = "Password is mandatory") @ValidPassword @ToString.Exclude String password,
    List<@FrenchLicensePlate String> licensePlates,
    // Physical card pickup location is optional as the customer can choose not to request a
    // physical card
    @Size(max = 150, message = "Pickup location must not exceed 150 characters")
        String pickupLocation) {}
