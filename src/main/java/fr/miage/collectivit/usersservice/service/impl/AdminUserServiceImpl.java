package fr.miage.collectivit.usersservice.service.impl;

import static java.util.Locale.ROOT;

import fr.miage.collectivit.usersservice.dto.AdminUserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.PageResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.repository.UserSpecifications;
import fr.miage.collectivit.usersservice.repository.UserStoreRepository;
import fr.miage.collectivit.usersservice.service.AdminUserService;
import fr.miage.collectivit.usersservice.service.UserRoleTransitionService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

  private final UserRepository userRepository;
  private final UserStoreRepository userStoreRepository;
  private final UserRoleTransitionService userRoleTransitionService;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<UserResponse> getUsers(
      String query,
      UserRole role,
      Boolean locked,
      Boolean eligibleForStore,
      Instant from,
      Instant to,
      Pageable pageable) {
    Specification<User> spec =
        UserSpecifications.withFilters(query, role, locked, eligibleForStore, from, to);
    Page<User> page = userRepository.findAll(spec, pageable);
    List<User> users = page.getContent();
    Map<Long, Integer> storeCounts = fetchStoreCounts(users);
    List<UserResponse> responses =
        users.stream()
            .map(u -> UserResponse.fromEntity(u, storeCounts.getOrDefault(u.getId(), 0)))
            .toList();
    return PageResponse.of(
        responses, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  private Map<Long, Integer> fetchStoreCounts(List<User> users) {
    Map<Long, Integer> counts = new HashMap<>();
    if (users.isEmpty()) {
      return counts;
    }
    List<Long> userIds = users.stream().map(User::getId).toList();
    for (Object[] row : userStoreRepository.countStoresByUserIds(userIds)) {
      if (row != null
          && row.length >= 2
          && row[0] instanceof Number uId
          && row[1] instanceof Number count) {
        counts.put(uId.longValue(), count.intValue());
      }
    }
    return counts;
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserById(UUID id) {
    User user = findUserOrThrow(id);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional
  @CacheEvict(value = "userProfiles", key = "#id")
  public UserResponse updateUser(UUID id, AdminUserUpdateRequest request) {
    User user = findUserOrThrow(id);
    applyProfileUpdates(user, request);

    User saved = userRepository.save(user);

    if (request.role() != null && request.role() != saved.getRole()) {
      userRoleTransitionService.changeUserRole(saved.getId(), request.role());
      saved = findUserOrThrow(id);
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Admin updated user id={} (publicId={}, role={})",
          saved.getId(),
          saved.getPublicId(),
          saved.getRole());
    }
    return UserResponse.fromEntity(saved);
  }

  private void applyProfileUpdates(User user, AdminUserUpdateRequest request) {
    updateEmailIfChanged(user, request.email());
    updateNamesIfPresent(user, request.firstName(), request.lastName());
  }

  private void updateEmailIfChanged(User user, String email) {
    if (email != null && !email.isBlank()) {
      String newEmail = email.trim().toLowerCase(ROOT);
      if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
        throw new EmailAlreadyExistsException(newEmail);
      }
      user.setEmail(newEmail);
    }
  }

  private void updateNamesIfPresent(User user, String firstName, String lastName) {
    if (firstName != null && !firstName.isBlank()) {
      user.setFirstName(firstName.trim());
    }
    if (lastName != null && !lastName.isBlank()) {
      user.setLastName(lastName.trim());
    }
  }

  @Override
  @Transactional
  public UserResponse unlockUser(UUID id) {
    User user = findUserOrThrow(id);
    user.resetLockout();
    User saved = userRepository.save(user);
    if (log.isInfoEnabled()) {
      log.info("Admin unlocked user id={} (publicId={})", saved.getId(), saved.getPublicId());
    }
    return UserResponse.fromEntity(saved);
  }

  private User findUserOrThrow(UUID id) {
    if (id == null) {
      throw new UserNotFoundException("User identifier must not be empty");
    }
    return userRepository.findByPublicId(id).orElseThrow(() -> new UserNotFoundException(id));
  }
}
