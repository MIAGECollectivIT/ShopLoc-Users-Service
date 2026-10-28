package fr.miage.collectivit.usersservice.dto;

import fr.miage.collectivit.usersservice.validation.FrenchLicensePlate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
    @NotBlank(message = "License plate is mandatory")
        @Size(max = 30, message = "License plate must not exceed 30 characters")
        @FrenchLicensePlate
        String licensePlate) {}
