package fr.miage.collectivit.usersservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserStoreLinkRequest(
    @NotNull(message = "L'identifiant du partenaire propriétaire est obligatoire")
        @Positive(message = "L'identifiant du partenaire propriétaire doit être valide")
        Long userId,
    @NotNull(message = "L'identifiant du commerce est obligatoire")
        @Positive(message = "L'identifiant du commerce doit être valide")
        Long storeId) {}
