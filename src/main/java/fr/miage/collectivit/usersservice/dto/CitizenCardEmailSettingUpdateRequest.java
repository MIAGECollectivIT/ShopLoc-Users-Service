package fr.miage.collectivit.usersservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CitizenCardEmailSettingUpdateRequest(
    @NotBlank(message = "L'adresse email du service citoyen est obligatoire")
        @Email(message = "Le format de l'adresse email est invalide")
        @Size(max = 255, message = "L'adresse email ne doit pas dépasser 255 caractères")
        String email) {}
