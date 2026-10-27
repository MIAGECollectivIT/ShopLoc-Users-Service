package fr.miage.collectivit.usersservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record UserUpdateRequest(
    @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
    @NotBlank(message = "Last name is mandatory")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
    @NotNull(message = "Birth date is mandatory") @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
    @Size(max = 150, message = "Pickup location must not exceed 150 characters")
        String pickupLocation) {}
