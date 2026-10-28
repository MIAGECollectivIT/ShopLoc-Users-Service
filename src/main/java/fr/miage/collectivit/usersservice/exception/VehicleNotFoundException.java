package fr.miage.collectivit.usersservice.exception;

public class VehicleNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VehicleNotFoundException(String vehicleId) {
    super("Vehicle not found with id or plate: " + vehicleId);
  }
}
