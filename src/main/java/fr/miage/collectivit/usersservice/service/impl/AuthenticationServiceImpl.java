package fr.miage.collectivit.usersservice.service.impl;

import static java.util.Locale.ROOT;

import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.AuthenticationService;
import fr.miage.collectivit.usersservice.util.ClientCodeGenerator;
import fr.miage.collectivit.usersservice.validation.FrenchLicensePlateValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final int MAX_REGISTRATION_RETRIES = 5;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ClientCodeGenerator clientCodeGenerator;
  private final TransactionTemplate transactionTemplate;

  @Override
  public UserResponse register(UserRegistrationRequest request) {
    String normalizedEmail = request.email().trim().toLowerCase(ROOT);
    checkEmailAvailability(normalizedEmail);

    String hashedPassword = passwordEncoder.encode(request.password());
    List<String> normalizedPlates = extractNormalizedPlates(request.licensePlates());

    User savedUser =
        persistWithClientCodeRetry(request, normalizedEmail, hashedPassword, normalizedPlates);
    return UserResponse.fromEntity(savedUser);
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
    String pickupLoc = request.pickupLocation() != null ? request.pickupLocation().trim() : null;
    return User.builder()
        .firstName(request.firstName().trim())
        .lastName(request.lastName().trim())
        .birthDate(request.birthDate())
        .email(email)
        .passwordHash(hashedPassword)
        .role(UserRole.CLIENT)
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
