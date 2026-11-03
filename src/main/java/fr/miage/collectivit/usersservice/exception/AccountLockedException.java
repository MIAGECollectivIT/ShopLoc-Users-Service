package fr.miage.collectivit.usersservice.exception;

public class AccountLockedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AccountLockedException(String message) {
    super(message);
  }
}
