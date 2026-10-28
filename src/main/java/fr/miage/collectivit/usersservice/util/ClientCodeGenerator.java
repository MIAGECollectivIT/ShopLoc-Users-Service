package fr.miage.collectivit.usersservice.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ClientCodeGenerator {

  public static final int CODE_LENGTH = 7;

  // Base62 character pool: digits (0-9), lowercase (a-z), uppercase (A-Z)
  private static final String ALPHANUMERIC_CHARS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private final SecureRandom secureRandom = new SecureRandom();

  public String generateCode() {
    StringBuilder builder = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      int index = secureRandom.nextInt(ALPHANUMERIC_CHARS.length());
      builder.append(ALPHANUMERIC_CHARS.charAt(index));
    }
    return builder.toString();
  }
}
