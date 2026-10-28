package fr.miage.collectivit.usersservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientCodeGeneratorTest {

  private final ClientCodeGenerator generator = new ClientCodeGenerator();

  @Test
  void shouldGenerateCodeWithExactLengthOfSeven() {
    String code = generator.generateCode();
    assertThat(code).hasSize(7);
  }

  @Test
  void shouldContainOnlyAlphanumericCharacters() {
    String code = generator.generateCode();
    assertThat(code).matches("^[a-zA-Z0-9]{7}$");
  }

  @Test
  void shouldGenerateDistinctCodes() {
    Set<String> generatedCodes = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      generatedCodes.add(generator.generateCode());
    }
    assertThat(generatedCodes).hasSize(50);
  }
}
