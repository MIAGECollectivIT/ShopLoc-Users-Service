package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Réponse d'authentification réussie avec jeton JWT")
public record AuthResponse(
    @Schema(
            description = "Jeton d'accès JWT signé",
            example =
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFsaWNlIER1cG9udCJ9...")
        String token,
    @Schema(description = "Type de jeton d'autorisation", example = "Bearer") String tokenType,
    @Schema(description = "Durée de validité du jeton en millisecondes", example = "7200000")
        long expiresIn,
    @Schema(description = "Profil de l'utilisateur authentifié") UserResponse user) {}
