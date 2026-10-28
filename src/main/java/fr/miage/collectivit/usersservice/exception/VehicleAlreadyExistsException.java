package fr.miage.collectivit.usersservice.exception;

public class VehicleAlreadyExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VehicleAlreadyExistsException(String licensePlate) {
    super("Vehicle with license plate '" + licensePlate + "' already exists for this user");
  }
}
