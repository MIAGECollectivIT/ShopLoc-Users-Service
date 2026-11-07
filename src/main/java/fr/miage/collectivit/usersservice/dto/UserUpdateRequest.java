package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.MinAge;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

@Builder
@Schema(description = "Données de mise à jour du profil utilisateur")
public record UserUpdateRequest(
    @Schema(
            description = "Prénom de l'utilisateur",
            example = "Alice",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
    @Schema(
            description = "Nom de famille de l'utilisateur",
            example = "Martin",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is mandatory")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
    @Schema(
            description = "Date de naissance (doit avoir au moins 18 ans)",
            example = "1995-04-20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be in the past")
        @MinAge(value = 18, message = "User must be at least 18 years old")
        LocalDate birthDate,
    // Physical card pickup location is optional as the customer can choose not to request a
    // physical card
    @Schema(
            description = "Lieu de retrait de la carte physique (optionnel)",
            example = "Point Relais Commerce Centre")
        @Size(max = 150, message = "Pickup location must not exceed 150 characters")
        String pickupLocation) {}
