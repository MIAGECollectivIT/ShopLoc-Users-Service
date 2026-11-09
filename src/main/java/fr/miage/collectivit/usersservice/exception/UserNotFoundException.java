package fr.miage.collectivit.usersservice.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UserNotFoundException(UUID userId) {
    super("User not found with id: " + userId);
  }

  public UserNotFoundException(String message) {
    super(message);
  }
}
