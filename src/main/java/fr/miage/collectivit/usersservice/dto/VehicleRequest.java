package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.FrenchLicensePlate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to add or update a vehicle")
public record VehicleRequest(
    @Schema(
            description = "Valid French license plate (format SIV AA-123-BB or FNI)",
            example = "AA-123-BB",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "License plate is mandatory")
        @Size(max = 30, message = "License plate must not exceed 30 characters")
        @FrenchLicensePlate
        String licensePlate) {}
