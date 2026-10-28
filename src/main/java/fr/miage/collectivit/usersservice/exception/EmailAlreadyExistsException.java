package fr.miage.collectivit.usersservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EmailAlreadyExistsException(String email) {
    super("A user with email '" + email + "' already exists");
  }

  public EmailAlreadyExistsException(String email, Throwable cause) {
    super("A user with email '" + email + "' already exists", cause);
  }
}
