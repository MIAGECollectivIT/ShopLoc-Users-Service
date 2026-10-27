package fr.miage.collectivit.usersservice.dto;

public record VehicleResponse(String vehicleId, String licensePlate) {
  public static VehicleResponse fromLicensePlate(String licensePlate) {
    return new VehicleResponse(licensePlate, licensePlate);
  }
}
