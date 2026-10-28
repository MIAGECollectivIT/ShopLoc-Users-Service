package fr.miage.collectivit.usersservice.exception;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String PROBLEM_BASE_URL = "https://errors.shoploc.com/";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed for request parameters");
    problem.setType(URI.create(PROBLEM_BASE_URL + "validation-error"));

    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }
    problem.setProperty("invalid_params", errors);
    return problem;
  }

  @ExceptionHandler({UserNotFoundException.class, VehicleNotFoundException.class})
  public ProblemDetail handleNotFoundException(RuntimeException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setType(URI.create(PROBLEM_BASE_URL + "not-found"));
    return problem;
  }

  @ExceptionHandler({EmailAlreadyExistsException.class, VehicleAlreadyExistsException.class})
  public ProblemDetail handleConflictException(RuntimeException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setType(URI.create(PROBLEM_BASE_URL + "conflict"));
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Database constraint violation occurred");
    problem.setType(URI.create(PROBLEM_BASE_URL + "data-integrity-conflict"));
    return problem;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String detail =
        String.format("Parameter '%s' has an invalid value: '%s'", ex.getName(), ex.getValue());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setType(URI.create(PROBLEM_BASE_URL + "type-mismatch"));
    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(URI.create(PROBLEM_BASE_URL + "bad-request"));
    return problem;
  }
}
