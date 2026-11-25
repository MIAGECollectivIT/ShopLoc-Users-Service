package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.entity.UserStore;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.exception.VehicleAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.VehicleNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.repository.UserStoreRepository;
import fr.miage.collectivit.usersservice.service.AdminPartnerService;
import fr.miage.collectivit.usersservice.service.UserProfileService;
import fr.miage.collectivit.usersservice.service.UserRoleTransitionService;
import fr.miage.collectivit.usersservice.validation.FrenchLicensePlateValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class UserProfileServiceImpl implements UserProfileService {

  private static final String CACHE_USER_PROFILES = "userProfiles";
  private static final String CACHE_KEY_USER_ID = "#userId";

  private final UserRepository userRepository;
  private final UserStoreRepository userStoreRepository;
  private final AdminPartnerService adminPartnerService;
  private final UserRoleTransitionService userRoleTransitionService;

  @Override
  @Transactional(readOnly = true)
  public UserResponse getCurrentUserProfile(String userIdHeader, String userEmailHeader) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public UserResponse getUserProfile(UUID userId) {
    User user = findUserByPublicId(userId);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional
  @CacheEvict(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public void linkStoreToUser(UUID userId, UUID storeUid) {
    if (userId == null || storeUid == null) {
      return;
    }
    User user = findUserByPublicId(userId);
    if (user.getRole() == UserRole.CLIENT) {
      userRoleTransitionService.changeUserRole(user.getId(), UserRole.MERCHANT);
      log.info("Promoted user publicId={} from CLIENT to MERCHANT on store link", userId);
    }
    if (userStoreRepository.existsByUserIdAndStoreUid(user.getId(), storeUid)) {
      if (log.isDebugEnabled()) {
        log.debug("Link already exists for userId={} and storeUid={}", userId, storeUid);
      }
      return;
    }
    UserStore link = UserStore.builder().userId(user.getId()).storeUid(storeUid).build();
    userStoreRepository.save(link);
    log.info("Link recorded between user publicId={} and store storeUid={}", userId, storeUid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerResponse> getPartners(String query, int limit) {
    return adminPartnerService.getPartners(query, limit);
  }

  @Override
  @Transactional
  @CacheEvict(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public UserResponse updateUserProfile(UUID userId, UserUpdateRequest request) {
    User user = findUserByPublicId(userId);
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setBirthDate(request.birthDate());
    if (user instanceof Client client) {
      client.setPickupLocation(
          (request.pickupLocation() != null && !request.pickupLocation().isBlank())
              ? request.pickupLocation().trim()
              : null);
    }

    User savedUser = userRepository.save(user);
    return UserResponse.fromEntity(savedUser);
  }

  @Override
  @Transactional
  @CacheEvict(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public void deleteUserProfile(UUID userId) {
    User user = findUserByPublicId(userId);
    userRepository.delete(user);
  }

  private Client getClientOrThrow(UUID userId) {
    User user = findUserByPublicId(userId);
    if (user instanceof Client client) {
      return client;
    }
    throw new ForbiddenException("Only clients can manage vehicles.");
  }

  private void validateNewPlateNotDuplicate(List<String> plates, String oldPlate, String newPlate) {
    if (!newPlate.equalsIgnoreCase(oldPlate) && hasPlate(plates, newPlate)) {
      throw new VehicleAlreadyExistsException(newPlate);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleResponse> getVehicles(UUID userId) {
    Client client = getClientOrThrow(userId);
    if (client.getLicensePlates() == null) {
      return List.of();
    }
    return client.getLicensePlates().stream().map(VehicleResponse::fromLicensePlate).toList();
  }

  @Override
  @Transactional
  @CacheEvict(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public VehicleResponse addVehicle(UUID userId, VehicleRequest request) {
    Client client = getClientOrThrow(userId);
    String plate = FrenchLicensePlateValidator.normalize(request.licensePlate());

    if (client.getLicensePlates() == null) {
      client.setLicensePlates(new ArrayList<>());
    }

    if (hasPlate(client.getLicensePlates(), plate)) {
      throw new VehicleAlreadyExistsException(plate);
    }

    client.getLicensePlates().add(plate);
    userRepository.save(client);
    return VehicleResponse.fromLicensePlate(plate);
  }

  @Override
  @Transactional
  @CacheEvict(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public void removeVehicle(UUID userId, String vehicleId) {
    Client client = getClientOrThrow(userId);
    if (client.getLicensePlates() == null || client.getLicensePlates().isEmpty()) {
      throw new VehicleNotFoundException(vehicleId);
    }

    String targetPlate = FrenchLicensePlateValidator.normalize(vehicleId);
    boolean removed =
        client.getLicensePlates().removeIf(plate -> plate.equalsIgnoreCase(targetPlate));
    if (!removed) {
      throw new VehicleNotFoundException(vehicleId);
    }

    userRepository.save(client);
  }

  @Override
  @Transactional
  @CacheEvict(value = CACHE_USER_PROFILES, key = CACHE_KEY_USER_ID)
  public VehicleResponse updateVehicle(UUID userId, String vehicleId, VehicleRequest request) {
    Client client = getClientOrThrow(userId);
    if (client.getLicensePlates() == null || client.getLicensePlates().isEmpty()) {
      throw new VehicleNotFoundException(vehicleId);
    }

    String oldPlate = FrenchLicensePlateValidator.normalize(vehicleId);
    int targetIndex = findPlateIndex(client.getLicensePlates(), oldPlate);
    if (targetIndex == -1) {
      throw new VehicleNotFoundException(vehicleId);
    }

    String newPlate = FrenchLicensePlateValidator.normalize(request.licensePlate());
    validateNewPlateNotDuplicate(client.getLicensePlates(), oldPlate, newPlate);

    client.getLicensePlates().set(targetIndex, newPlate);
    userRepository.save(client);
    return VehicleResponse.fromLicensePlate(newPlate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleResponse> getCurrentUserVehicles(String userIdHeader, String userEmailHeader) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader);
    return getVehicles(user.getPublicId());
  }

  @Override
  @Transactional
  public VehicleResponse addCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, VehicleRequest request) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader);
    return addVehicle(user.getPublicId(), request);
  }

  @Override
  @Transactional
  public VehicleResponse updateCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, String vehicleId, VehicleRequest request) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader);
    return updateVehicle(user.getPublicId(), vehicleId, request);
  }

  @Override
  @Transactional
  public void removeCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, String vehicleId) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader);
    removeVehicle(user.getPublicId(), vehicleId);
  }

  private int findPlateIndex(List<String> plates, String targetPlate) {
    for (int i = 0; i < plates.size(); i++) {
      if (plates.get(i).equalsIgnoreCase(targetPlate)) {
        return i;
      }
    }
    return -1;
  }

  private boolean hasPlate(List<String> plates, String targetPlate) {
    return plates.stream().anyMatch(p -> p.equalsIgnoreCase(targetPlate));
  }

  private User findUserByPublicId(UUID userId) {
    return userRepository
        .findByPublicId(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
  }
}
