package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.FrenchLicensePlate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requête d'ajout ou mise à jour d'un véhicule")
public record VehicleRequest(
    @Schema(
            description = "Plaque d'immatriculation française valide (format SIV AA-123-BB ou FNI)",
            example = "AA-123-BB",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "License plate is mandatory")
        @Size(max = 30, message = "License plate must not exceed 30 characters")
        @FrenchLicensePlate
        String licensePlate) {}
