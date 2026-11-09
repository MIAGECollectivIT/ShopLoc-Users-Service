package fr.miage.collectivit.usersservice.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidPasswordValidatorTest {

  private ValidPasswordValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new ValidPasswordValidator();
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  void shouldAcceptCompliantPassword() {
    assertThat(validator.isValid("CorrectP@ssword123!", context)).isTrue();
    assertThat(validator.isValid("Str0ng#Passw0rd", context)).isTrue();
    assertThat(validator.isValid("Super!Secret_2026", context)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "", // empty
        "Short1!", // too short (< 12)
        "EightChar1!", // 11 chars
        "alllowercase123!", // no uppercase
        "ALLUPPERCASE123!", // no lowercase
        "NoDigitsHereP@ss!", // no digit
        "NoSpecialChars1234", // no special char
      })
  void shouldRejectNonCompliantPasswords(String password) {
    assertThat(validator.isValid(password, context)).isFalse();
  }

  @Test
  void shouldRejectNull() {
    assertThat(validator.isValid(null, context)).isFalse();
  }
}
