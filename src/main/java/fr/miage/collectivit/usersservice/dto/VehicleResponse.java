package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered vehicle details")
public record VehicleResponse(
    @Schema(description = "Unique vehicle identifier", example = "AA-123-BB") String vehicleId,
    @Schema(description = "French license plate number", example = "AA-123-BB")
        String licensePlate) {
  public static VehicleResponse fromLicensePlate(String licensePlate) {
    return new VehicleResponse(licensePlate, licensePlate);
  }
}
