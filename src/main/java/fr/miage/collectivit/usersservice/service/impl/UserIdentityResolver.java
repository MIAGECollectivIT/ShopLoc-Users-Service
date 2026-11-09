package fr.miage.collectivit.usersservice.service.impl;

import static java.util.Locale.ROOT;

import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import java.util.UUID;

final class UserIdentityResolver {

  private UserIdentityResolver() {
    // Utility class
  }

  static User resolveUser(
      UserRepository userRepository, String userIdHeader, String userEmailHeader) {
    if (isBlank(userIdHeader) && isBlank(userEmailHeader)) {
      throw new UnauthorizedException(
          "Missing authentication identity headers (X-User-Id or X-User-Email)");
    }

    User user = findByIdentifier(userRepository, userIdHeader);
    if (user == null) {
      user = findByEmail(userRepository, userEmailHeader);
    }
    if (user == null) {
      throw new UserNotFoundException("User not found for provided identity");
    }
    return user;
  }

  private static User findByIdentifier(UserRepository userRepository, String idHeader) {
    if (isBlank(idHeader)) {
      return null;
    }
    String trimmedId = idHeader.trim();
    try {
      return userRepository.findByPublicId(UUID.fromString(trimmedId)).orElse(null);
    } catch (IllegalArgumentException notUuid) {
      return parseNumericId(userRepository, trimmedId);
    }
  }

  private static User parseNumericId(UserRepository userRepository, String trimmedId) {
    try {
      return userRepository.findById(Long.parseLong(trimmedId)).orElse(null);
    } catch (NumberFormatException notNumeric) {
      return null;
    }
  }

  private static User findByEmail(UserRepository userRepository, String emailHeader) {
    if (isBlank(emailHeader)) {
      return null;
    }
    return userRepository.findByEmail(emailHeader.trim().toLowerCase(ROOT)).orElse(null);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
