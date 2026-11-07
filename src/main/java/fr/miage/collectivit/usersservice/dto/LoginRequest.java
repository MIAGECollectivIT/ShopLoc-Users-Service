package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.ToString;

@Builder
@Schema(description = "Requête d'authentification utilisateur")
public record LoginRequest(
    @Schema(
            description = "Adresse email du compte utilisateur",
            example = "alice.dupont@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Format d'adresse email invalide")
        String email,
    @Schema(
            description = "Mot de passe en clair du compte",
            example = "SecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le mot de passe est obligatoire")
        @ToString.Exclude
        String password) {}
