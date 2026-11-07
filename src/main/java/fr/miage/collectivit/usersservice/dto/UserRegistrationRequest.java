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
@Schema(description = "Données d'inscription d'un nouvel utilisateur client")
public record UserRegistrationRequest(
    @Schema(
            description = "Prénom de l'utilisateur",
            example = "Alice",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is mandatory")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
    @Schema(
            description = "Nom de famille de l'utilisateur",
            example = "Dupont",
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
    @Schema(
            description = "Adresse email unique",
            example = "alice.dupont@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
    @Schema(
            description =
                "Mot de passe sécurisé (au moins 12 caractères, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial)",
            example = "SecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is mandatory")
        @ValidPassword
        @ToString.Exclude
        String password,
    @ArraySchema(
            schema =
                @Schema(
                    description =
                        "Plaque d'immatriculation française valide (format AA-123-BB ou FNI)",
                    example = "AA-123-BB"))
        List<@FrenchLicensePlate String> licensePlates,
    // Physical card pickup location is optional as the customer can choose not to request a
    // physical card
    @Schema(
            description = "Lieu de retrait de la carte physique (optionnel)",
            example = "Mairie de Lille")
        @Size(max = 150, message = "Pickup location must not exceed 150 characters")
        String pickupLocation) {}
