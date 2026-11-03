package fr.miage.collectivit.usersservice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes matching the application's security configuration (strength 12).
 * Can be executed directly from IDE or CLI.
 */
public final class PasswordHashGenerator {

  public static final int BCRYPT_STRENGTH = 12;

  private final PasswordEncoder passwordEncoder;

  public PasswordHashGenerator() {
    this(new BCryptPasswordEncoder(BCRYPT_STRENGTH));
  }

  public PasswordHashGenerator(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Hashes the raw password using BCrypt with strength 12.
   *
   * @param rawPassword the plain-text password to hash
   * @return the BCrypt hash string
   */
  public String hashPassword(String rawPassword) {
    if (rawPassword == null || rawPassword.isEmpty()) {
      throw new IllegalArgumentException("Password cannot be null or empty");
    }
    return passwordEncoder.encode(rawPassword);
  }

  /**
   * Standalone entrypoint to generate and print BCrypt hashes directly to standard output.
   *
   * @param args passwords to hash. If none provided, displays usage and an example hash.
   */
  @SuppressWarnings("PMD.SystemPrintln")
  public static void main(String[] args) {
    PasswordHashGenerator generator = new PasswordHashGenerator();

    if (args == null || args.length == 0) {
      System.out.println("Usage: java PasswordHashGenerator <password1> [password2] ...");
      System.out.println("Example hash for: DevPassword123!");
      String defaultHash = generator.hashPassword("DevPassword123!");
      System.out.println("Hash: " + defaultHash);
      return;
    }

    for (String raw : args) {
      String hash = generator.hashPassword(raw);
      System.out.println("Password: " + raw);
      System.out.println("Hash:     " + hash);
      System.out.println();
    }
  }
}
