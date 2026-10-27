package fr.miage.collectivit.usersservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

  private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

  @Test
  void shouldEncodePasswordWithBCryptFormat() {
    String rawPassword = "SuperSecretPassword123!";
    String encoded = passwordEncoder.encode(rawPassword);

    assertThat(encoded).isNotBlank();
    // Standard BCrypt hashes start with $2a$ or $2b$
    assertThat(encoded).startsWith("$2a$");
  }

  @Test
  void shouldProduceDifferentHashesDueToRandomSalting() {
    String rawPassword = "SamePassword123!";
    String hash1 = passwordEncoder.encode(rawPassword);
    String hash2 = passwordEncoder.encode(rawPassword);

    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
    assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
  }

  @Test
  void shouldRejectIncorrectPassword() {
    String rawPassword = "CorrectPassword123!";
    String encoded = passwordEncoder.encode(rawPassword);

    assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
  }
}
