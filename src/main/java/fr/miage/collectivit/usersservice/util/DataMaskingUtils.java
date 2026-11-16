package fr.miage.collectivit.usersservice.util;

import java.util.Locale;

/**
 * Utility for masking personally identifiable information (PII) in compliance with GDPR data
 * minimization principles.
 */
public final class DataMaskingUtils {

  private static final String MASK_PLACEHOLDER = "***";
  private static final String SHORT_MASK_PLACEHOLDER = "*";
  private static final int SINGLE_CHAR_LENGTH = 1;
  private static final int SHORT_TEXT_LENGTH = 2;

  private DataMaskingUtils() {
    // Utility class
  }

  /**
   * Masks a last name, preserving initial and final characters when appropriate. Examples: -
   * "Dupont" -> "D***t" - "Do" -> "D*" - "D" -> "D*"
   */
  public static String maskLastName(String lastName) {
    if (lastName == null || lastName.isBlank()) {
      return "";
    }
    String trimmed = lastName.trim();
    if (trimmed.length() <= SINGLE_CHAR_LENGTH) {
      return trimmed.toUpperCase(Locale.ROOT) + SHORT_MASK_PLACEHOLDER;
    }
    if (trimmed.length() == SHORT_TEXT_LENGTH) {
      return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + SHORT_MASK_PLACEHOLDER;
    }
    char first = Character.toUpperCase(trimmed.charAt(0));
    char last = Character.toLowerCase(trimmed.charAt(trimmed.length() - 1));
    return first + MASK_PLACEHOLDER + last;
  }

  /**
   * Masks an email address while preserving domain context for verification. Examples: -
   * "alice.dupont@example.com" -> "a***t@example.com" - "ab@domain.com" -> "a*@domain.com" -
   * "a@domain.com" -> "a***@domain.com"
   */
  public static String maskEmail(String email) {
    if (email == null || email.isBlank()) {
      return "";
    }
    String trimmed = email.trim();
    int atIndex = trimmed.indexOf('@');
    if (atIndex <= 0 || atIndex == trimmed.length() - 1) {
      return trimmed.charAt(0) + MASK_PLACEHOLDER;
    }

    String localPart = trimmed.substring(0, atIndex);
    String domainPart = trimmed.substring(atIndex).toLowerCase(Locale.ROOT);

    String maskedLocal;
    if (localPart.length() <= SINGLE_CHAR_LENGTH) {
      maskedLocal = localPart.charAt(0) + MASK_PLACEHOLDER;
    } else if (localPart.length() == SHORT_TEXT_LENGTH) {
      maskedLocal = localPart.charAt(0) + SHORT_MASK_PLACEHOLDER;
    } else {
      maskedLocal =
          localPart.charAt(0) + MASK_PLACEHOLDER + localPart.charAt(localPart.length() - 1);
    }

    return maskedLocal + domainPart;
  }
}
