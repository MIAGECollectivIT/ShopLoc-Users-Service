package fr.miage.collectivit.usersservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/** Validator ensuring a birth date meets the minimum age requirement. */
public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {

  private int minAge;

  @Override
  public void initialize(MinAge constraintAnnotation) {
    this.minAge = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
    if (birthDate == null) {
      return true;
    }
    LocalDate today = LocalDate.now();
    LocalDate maxBirthDateForAdult = today.minusYears(minAge);
    return !birthDate.isAfter(maxBirthDateForAdult);
  }
}
