package fr.miage.collectivit.usersservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

  private static final int MIN_LENGTH = 12;
  private static final int MAX_LENGTH = 128;

  private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
  private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
  private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

  private static final Set<String> COMMON_WEAK_PASSWORDS =
      Set.of(
          "password123!",
          "azerty123456!",
          "qwerty123456!",
          "admin123456!",
          "welcome12345!",
          "supersecret1!");

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null
        || password.isBlank()
        || password.length() < MIN_LENGTH
        || password.length() > MAX_LENGTH) {
      return false;
    }

    return hasRequiredCharacterTypes(password) && !isCommonWeak(password, context);
  }

  private boolean hasRequiredCharacterTypes(String password) {
    return LOWERCASE_PATTERN.matcher(password).find()
        && UPPERCASE_PATTERN.matcher(password).find()
        && DIGIT_PATTERN.matcher(password).find()
        && SPECIAL_CHAR_PATTERN.matcher(password).find();
  }

  private boolean isCommonWeak(String password, ConstraintValidatorContext context) {
    String lower = password.toLowerCase(Locale.ROOT);
    if (COMMON_WEAK_PASSWORDS.contains(lower)) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "This password is too common or easily guessable, please choose a stronger password.")
          .addConstraintViolation();
      return true;
    }
    return false;
  }
}
