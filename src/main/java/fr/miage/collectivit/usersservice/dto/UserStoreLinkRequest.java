package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Requête de liaison entre un commerçant partenaire et un magasin")
public record UserStoreLinkRequest(
    @Schema(
            description = "Identifiant interne de l'utilisateur commerçant propriétaire",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "L'identifiant du partenaire propriétaire est obligatoire")
        @Positive(message = "L'identifiant du partenaire propriétaire doit être valide")
        Long userId,
    @Schema(
            description = "Identifiant interne du commerce à rattacher",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "L'identifiant du commerce est obligatoire")
        @Positive(message = "L'identifiant du commerce doit être valide")
        Long storeId) {}
