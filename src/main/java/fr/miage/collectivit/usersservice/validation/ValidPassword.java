package fr.miage.collectivit.usersservice.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Validates that a password adheres to CNIL recommendations: - Minimum 12 characters (maximum 128
 * characters) - Contains at least one uppercase letter, one lowercase letter, one digit, and one
 * special character - Is not a notoriously weak common password
 */
@Documented
@Constraint(validatedBy = ValidPasswordValidator.class)
@Target({FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface ValidPassword {

  String message() default
      "Le mot de passe doit comporter au moins 12 caractères et inclure au moins une lettre majuscule, une lettre minuscule, un chiffre et un caractère spécial (recommandations CNIL).";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
