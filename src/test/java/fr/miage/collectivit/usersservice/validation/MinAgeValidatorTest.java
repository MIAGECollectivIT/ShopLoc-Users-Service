package fr.miage.collectivit.usersservice.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinAgeValidatorTest {

  private MinAgeValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new MinAgeValidator();
    MinAge minAgeAnnotation = mock(MinAge.class);
    when(minAgeAnnotation.value()).thenReturn(18);
    validator.initialize(minAgeAnnotation);
    context = mock(ConstraintValidatorContext.class);
  }

  @Test
  void shouldBeValidWhenBirthDateIsNull() {
    assertThat(validator.isValid(null, context)).isTrue();
  }

  @Test
  void shouldBeValidWhenUserIsExactly18YearsOldToday() {
    LocalDate exactly18 = LocalDate.now().minusYears(18);
    assertThat(validator.isValid(exactly18, context)).isTrue();
  }

  @Test
  void shouldBeValidWhenUserIsOlderThan18() {
    LocalDate age25 = LocalDate.now().minusYears(25);
    assertThat(validator.isValid(age25, context)).isTrue();
  }

  @Test
  void shouldBeInvalidWhenUserIsUnder18() {
    LocalDate age16 = LocalDate.now().minusYears(16);
    assertThat(validator.isValid(age16, context)).isFalse();
  }

  @Test
  void shouldBeInvalidWhenUserIsOneDayAwayFrom18thBirthday() {
    LocalDate oneDayBefore18 = LocalDate.now().minusYears(18).plusDays(1);
    assertThat(validator.isValid(oneDayBefore18, context)).isFalse();
  }

  @Test
  void shouldBeInvalidWhenBirthDateIsInTheFuture() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    assertThat(validator.isValid(tomorrow, context)).isFalse();
  }
}
