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

/** Validates that a license plate conforms to French standards (SIV or FNI formats). */
@Documented
@Constraint(validatedBy = FrenchLicensePlateValidator.class)
@Target({FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface FrenchLicensePlate {

  String message() default
      "License plate does not conform to French standards (e.g. AA-123-AA or 1234 AB 75)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
