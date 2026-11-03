package fr.miage.collectivit.usersservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGeneratorTest {

  private final PasswordHashGenerator generator = new PasswordHashGenerator();
  private final BCryptPasswordEncoder encoder =
      new BCryptPasswordEncoder(PasswordHashGenerator.BCRYPT_STRENGTH);

  @Test
  void shouldGenerateValidBcryptHashMatchingPassword() {
    String raw = "StrongP@ssw0rd123!";
    String hash = generator.hashPassword(raw);

    assertThat(hash).isNotNull().startsWith("$2a$12$");
    assertThat(encoder.matches(raw, hash)).isTrue();
  }

  @Test
  void shouldThrowExceptionWhenPasswordIsNullOrEmpty() {
    assertThatThrownBy(() -> generator.hashPassword(null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> generator.hashPassword(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldExecuteMainWithoutThrowing() {
    PasswordHashGenerator.main(new String[] {});
    PasswordHashGenerator.main(new String[] {"TestPass123!"});
  }
}
