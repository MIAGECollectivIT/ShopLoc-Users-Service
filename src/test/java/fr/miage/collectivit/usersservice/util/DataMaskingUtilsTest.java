package fr.miage.collectivit.usersservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DataMaskingUtilsTest {

  @Test
  @DisplayName("Should mask last name according to GDPR rules")
  void shouldMaskLastName() {
    assertThat(DataMaskingUtils.maskLastName("Dupont")).isEqualTo("D***t");
    assertThat(DataMaskingUtils.maskLastName("garnier")).isEqualTo("G***r");
    assertThat(DataMaskingUtils.maskLastName("Do")).isEqualTo("D*");
    assertThat(DataMaskingUtils.maskLastName("A")).isEqualTo("A*");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t", "\n"})
  @DisplayName("Should return empty string for null, empty or blank last names")
  void shouldReturnEmptyForBlankLastName(String input) {
    assertThat(DataMaskingUtils.maskLastName(input)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "alice.dupont@example.com, a***t@example.com",
    "luc@shoploc.fr, l***c@shoploc.fr",
    "ab@domain.com, a*@domain.com",
    "a@domain.com, a***@domain.com"
  })
  @DisplayName("Should mask email while keeping domain intact")
  void shouldMaskEmail(String input, String expected) {
    assertThat(DataMaskingUtils.maskEmail(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "invalid-no-at", "invalid@"})
  @DisplayName("Should handle edge cases in email masking")
  void shouldHandleInvalidEmailsGracefully(String input) {
    if (input == null || input.isBlank()) {
      assertThat(DataMaskingUtils.maskEmail(input)).isEmpty();
    } else {
      assertThat(DataMaskingUtils.maskEmail(input)).contains("***");
    }
  }
}
