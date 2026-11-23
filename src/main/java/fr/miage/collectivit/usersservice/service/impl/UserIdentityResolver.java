package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.UnauthorizedException;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import java.util.UUID;

final class UserIdentityResolver {

  private UserIdentityResolver() {
    // Utility class
  }

  static User resolveUser(UserRepository userRepository, String userIdHeader) {
    if (userIdHeader == null || userIdHeader.isBlank()) {
      throw new UnauthorizedException("Missing authentication identity header (X-User-Id)");
    }

    UUID publicId = UUID.fromString(userIdHeader.trim());
    return userRepository
        .findByPublicId(publicId)
        .orElseThrow(() -> new UserNotFoundException(publicId));
  }
}
