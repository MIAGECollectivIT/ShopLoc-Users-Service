package fr.miage.collectivit.usersservice.service.impl;

import static java.util.Locale.ROOT;

import fr.miage.collectivit.usersservice.dto.AuthResponse;
import fr.miage.collectivit.usersservice.dto.LoginRequest;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.AccountLockedException;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.security.JwtTokenService;
import fr.miage.collectivit.usersservice.service.AuthenticationService;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import fr.miage.collectivit.usersservice.validation.FrenchLicensePlateValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@SuppressWarnings("PMD.TooManyMethods")
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final int MAX_REGISTRATION_RETRIES = 5;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ClientCodeGenerator clientCodeGenerator;
  private final TransactionTemplate transactionTemplate;
  private final JwtTokenService jwtTokenService;
  private final int maxFailedAttempts;
  private final long lockoutMinutes;

  public AuthenticationServiceImpl(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      ClientCodeGenerator clientCodeGenerator,
      TransactionTemplate transactionTemplate,
      JwtTokenService jwtTokenService,
      @Value("${application.security.lockout.max-attempts:5}") int maxFailedAttempts,
      @Value("${application.security.lockout.lockout-minutes:15}") long lockoutMinutes) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clientCodeGenerator = clientCodeGenerator;
    this.transactionTemplate = transactionTemplate;
    this.jwtTokenService = jwtTokenService;
    this.maxFailedAttempts = maxFailedAttempts;
    this.lockoutMinutes = lockoutMinutes;
  }

  @Override
  public UserResponse register(UserRegistrationRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase(ROOT);
    checkEmailAvailability(normalizedEmail);
    validatePasswordPersonalContext(
        request.password(), request.firstName(), request.lastName(), normalizedEmail);

    String hashedPassword = passwordEncoder.encode(request.password());
    List<String> normalizedPlates = extractNormalizedPlates(request.licensePlates());

    User savedUser =
        persistWithClientCodeRetry(request, normalizedEmail, hashedPassword, normalizedPlates);
    return UserResponse.fromEntity(savedUser);
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase(ROOT);
    User user =
        userRepository
            .findByEmail(normalizedEmail)
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        "Invalid credentials (incorrect email address or password)"));

    if (user.isLocked()) {
      throw new AccountLockedException(
          "Account temporarily locked due to excessive authentication failures. Please try again later.");
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      user.incrementFailedAttempts(maxFailedAttempts, lockoutMinutes);
      userRepository.save(user);

      if (user.isLocked()) {
        throw new AccountLockedException(
            "Account temporarily locked due to excessive consecutive failures ("
                + maxFailedAttempts
                + " attempts). Please try again in "
                + lockoutMinutes
                + " minutes.");
      }

      throw new UnauthorizedException("Invalid credentials (incorrect email address or password)");
    }

    if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
      user.resetLockout();
      userRepository.save(user);
    }

    String token = jwtTokenService.generateToken(user);
    return AuthResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .expiresIn(jwtTokenService.getExpirationSeconds())
        .user(UserResponse.fromEntity(user))
        .build();
  }

  private void validatePasswordPersonalContext(
      String password, String firstName, String lastName, String email) {
    if (password == null) {
      return;
    }
    String lowerPassword = password.toLowerCase(ROOT);
    checkName(lowerPassword, firstName, "first name");
    checkName(lowerPassword, lastName, "last name");
    checkEmailLocalPart(lowerPassword, email);
  }

  private void checkName(String lowerPassword, String name, String fieldLabel) {
    if (name != null
        && name.trim().length() >= 3
        && lowerPassword.contains(name.trim().toLowerCase(ROOT))) {
      throw new IllegalArgumentException(
          "Password must not contain your " + fieldLabel + " (CNIL recommendation).");
    }
  }

  private void checkEmailLocalPart(String lowerPassword, String email) {
    if (email != null && email.contains("@")) {
      String localPart = email.substring(0, email.indexOf('@')).trim().toLowerCase(ROOT);
      if (localPart.length() >= 3 && lowerPassword.contains(localPart)) {
        throw new IllegalArgumentException(
            "Password must not contain your email address (CNIL recommendation).");
      }
    }
  }

  private void checkEmailAvailability(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }
  }

  private List<String> extractNormalizedPlates(List<String> plates) {
    if (plates == null) {
      return new ArrayList<>();
    }
    return plates.stream()
        .filter(plate -> plate != null && !plate.isBlank())
        .map(FrenchLicensePlateValidator::normalize)
        .distinct()
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private User persistWithClientCodeRetry(
      UserRegistrationRequest request, String email, String hashedPassword, List<String> plates) {
    for (int attempt = 1; attempt <= MAX_REGISTRATION_RETRIES; attempt++) {
      User user = buildUserEntity(request, email, hashedPassword, plates);
      try {
        User savedUser = transactionTemplate.execute(status -> userRepository.saveAndFlush(user));
        if (savedUser != null) {
          return savedUser;
        }
      } catch (DataIntegrityViolationException ex) {
        handleSaveConflict(email, attempt, ex);
      }
    }
    throw new IllegalStateException("Failed to generate a unique client code after retries");
  }

  private void handleSaveConflict(String email, int attempt, DataIntegrityViolationException ex) {
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email, ex);
    }
    if (attempt == MAX_REGISTRATION_RETRIES) {
      throw ex;
    }
  }

  private User buildUserEntity(
      UserRegistrationRequest request, String email, String hashedPassword, List<String> plates) {
    String pickupLoc =
        (request.pickupLocation() != null && !request.pickupLocation().isBlank())
            ? request.pickupLocation().trim()
            : null;
    return Client.builder()
        .firstName(request.firstName().trim())
        .lastName(request.lastName().trim())
        .birthDate(request.birthDate())
        .email(email)
        .passwordHash(hashedPassword)
        .clientCode(generateUniqueClientCode())
        .pickupLocation(pickupLoc)
        .licensePlates(plates)
        .build();
  }

  private String generateUniqueClientCode() {
    String code;
    do {
      code = clientCodeGenerator.generateCode();
    } while (userRepository.existsByClientCode(code));
    return code;
  }
}
