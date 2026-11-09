package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.FrenchLicensePlate;
import fr.miage.collectivit.usersservice.validation.MinAge;
import fr.miage.collectivit.usersservice.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Registration payload for a new client user")
public record UserRegistrationRequest(
    @Schema(
            description = "User first name",
            example = "Alice",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
    @Schema(
            description = "User last name",
            example = "Dupont",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is mandatory")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
    @Schema(
            description = "Birth date (must be at least 18 years old)",
            example = "1995-04-20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be in the past")
        @MinAge(value = 18, message = "User must be at least 18 years old")
        LocalDate birthDate,
    @Schema(
            description = "Unique email address",
            example = "alice.dupont@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
    @Schema(
            description =
                "Secure password (at least 12 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character)",
            example = "SecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is mandatory")
        @ValidPassword
        @ToString.Exclude
        String password,
    @ArraySchema(
            schema =
                @Schema(
                    description = "Valid French license plate (format AA-123-BB or FNI)",
                    example = "AA-123-BB"))
        List<@FrenchLicensePlate String> licensePlates,
    // Physical card pickup location is optional as the customer can choose not to request a
    // physical card
    @Schema(description = "Physical card pickup location (optional)", example = "Mairie de Lille")
        @Size(max = 150, message = "Pickup location must not exceed 150 characters")
        String pickupLocation) {}
