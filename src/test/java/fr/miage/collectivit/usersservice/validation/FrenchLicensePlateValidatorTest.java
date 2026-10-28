package fr.miage.collectivit.usersservice.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FrenchLicensePlateValidatorTest {

  private FrenchLicensePlateValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new FrenchLicensePlateValidator();
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  void shouldBeValidWhenPlateIsNull() {
    assertThat(validator.isValid(null, context)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "AA-123-AA",
        "aa-123-aa",
        "AA 123 AA",
        "AA123AA",
        "WW-456-WW",
        "ZZ-999-ZZ",
        "1234 AB 75",
        "123 ABC 92",
        "45 A 2A",
        "45 A 2B",
        "1234AB75",
        "1234-AB-75"
      })
  void shouldAcceptValidFrenchLicensePlates(String validPlate) {
    assertThat(validator.isValid(validPlate, context)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "   ",
        "123",
        "ABCDEF",
        "AA-123-OO", // O is forbidden in SIV
        "AA-123-II", // I is forbidden in SIV
        "AA-123-UU", // U is forbidden in SIV
        "123456789",
        "INVALID-PLATE-TOO-LONG"
      })
  void shouldRejectInvalidFrenchLicensePlates(String invalidPlate) {
    assertThat(validator.isValid(invalidPlate, context)).isFalse();
  }

  @Test
  void shouldNormalizeSivPlates() {
    assertThat(FrenchLicensePlateValidator.normalize("aa-123-aa")).isEqualTo("AA-123-AA");
    assertThat(FrenchLicensePlateValidator.normalize("aa123aa")).isEqualTo("AA-123-AA");
    assertThat(FrenchLicensePlateValidator.normalize("AA 123 AA")).isEqualTo("AA-123-AA");
  }

  @Test
  void shouldNormalizeFniPlates() {
    assertThat(FrenchLicensePlateValidator.normalize("1234ab75")).isEqualTo("1234 AB 75");
    assertThat(FrenchLicensePlateValidator.normalize("1234 AB 75")).isEqualTo("1234 AB 75");
    assertThat(FrenchLicensePlateValidator.normalize("45a2a")).isEqualTo("45 A 2A");
  }
}
