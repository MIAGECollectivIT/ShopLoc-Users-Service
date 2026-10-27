package fr.miage.collectivit.usersservice.validation;

import static java.util.Locale.ROOT;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates vehicle license plates according to French standards: - SIV (Système d'Immatriculation
 * des Véhicules, since 2009): AA-123-AA (excluding I, O, U) - FNI (Fichier National des
 * Immatriculations, 1950-2009): 1234 AB 75
 */
public class FrenchLicensePlateValidator
    implements ConstraintValidator<FrenchLicensePlate, String> {

  private static final Pattern SIV_PATTERN =
      Pattern.compile(
          "^[A-HJ-NP-TV-Z]{2}[-\\s]?[0-9]{3}[-\\s]?[A-HJ-NP-TV-Z]{2}$", Pattern.CASE_INSENSITIVE);

  private static final Pattern FNI_PATTERN =
      Pattern.compile(
          "^[0-9]{1,4}[-\\s]?[A-Z]{1,3}[-\\s]?(?:[0-9]{2,3}|2[AB])$", Pattern.CASE_INSENSITIVE);

  private static final Pattern SIV_CLEAN_PATTERN =
      Pattern.compile("^[A-HJ-NP-TV-Z]{2}[0-9]{3}[A-HJ-NP-TV-Z]{2}$");

  private static final Pattern FNI_CLEAN_PATTERN =
      Pattern.compile("^([0-9]{1,4})([A-Z]{1,3})([0-9]{2,3}|2[AB])$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (value.isBlank()) {
      return false;
    }
    String trimmed = value.trim();
    return SIV_PATTERN.matcher(trimmed).matches() || FNI_PATTERN.matcher(trimmed).matches();
  }

  /**
   * Normalizes a French license plate into standard display format with hyphens or spaces. - SIV:
   * "AA-123-AA" - FNI: "1234 AB 75"
   */
  public static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String raw = value.trim().toUpperCase(ROOT);
    String clean = raw.replaceAll("[-\\s]", "");

    if (SIV_CLEAN_PATTERN.matcher(clean).matches()) {
      return clean.substring(0, 2) + "-" + clean.substring(2, 5) + "-" + clean.substring(5, 7);
    }

    Matcher fniMatcher = FNI_CLEAN_PATTERN.matcher(clean);
    if (fniMatcher.matches()) {
      return fniMatcher.group(1) + " " + fniMatcher.group(2) + " " + fniMatcher.group(3);
    }

    return raw;
  }
}
