package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informations sur un véhicule enregistré")
public record VehicleResponse(
    @Schema(description = "Identifiant unique du véhicule", example = "AA-123-BB") String vehicleId,
    @Schema(description = "Numéro de plaque d'immatriculation française", example = "AA-123-BB")
        String licensePlate) {
  public static VehicleResponse fromLicensePlate(String licensePlate) {
    return new VehicleResponse(licensePlate, licensePlate);
  }
}
