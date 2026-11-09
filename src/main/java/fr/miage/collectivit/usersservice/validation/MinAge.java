package fr.miage.collectivit.usersservice.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Validates that a birth date corresponds to an age greater than or equal to the specified minimum.
 */
@Documented
@Constraint(validatedBy = MinAgeValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface MinAge {

  String message() default "User must be at least {value} years old";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int value() default 18;
}
