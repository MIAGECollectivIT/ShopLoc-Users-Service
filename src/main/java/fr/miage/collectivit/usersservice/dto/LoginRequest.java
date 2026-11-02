package fr.miage.collectivit.usersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.ToString;

@Builder
public record LoginRequest(
    @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Format d'adresse email invalide")
        String email,
    @NotBlank(message = "Le mot de passe est obligatoire") @ToString.Exclude String password) {}
