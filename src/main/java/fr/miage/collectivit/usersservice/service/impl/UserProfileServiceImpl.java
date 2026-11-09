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
import fr.miage.collectivit.usersservice.service.UserProfileService;
import fr.miage.collectivit.usersservice.validation.FrenchLicensePlateValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class UserProfileServiceImpl implements UserProfileService {

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private final UserRepository userRepository;
  private final UserStoreRepository userStoreRepository;

  @Override
  @Transactional(readOnly = true)
  public UserResponse getCurrentUserProfile(String userIdHeader, String userEmailHeader) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader, userEmailHeader);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserProfile(UUID userId) {
    User user = findUserByPublicId(userId);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserProfileByIdentifier(String identifier) {
    User user = UserIdentityResolver.resolveUser(userRepository, identifier, identifier);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional
  public void linkStoreToUser(Long userId, Long storeId) {
    if (userId == null || storeId == null) {
      return;
    }
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException("User not found with id: " + userId);
    }
    if (userStoreRepository.existsByUserIdAndStoreId(userId, storeId)) {
      if (log.isDebugEnabled()) {
        log.debug("Link already exists for userId={} and storeId={}", userId, storeId);
      }
      return;
    }
    UserStore link = UserStore.builder().userId(userId).storeId(storeId).build();
    userStoreRepository.save(link);
    log.info("Link recorded between user id={} and store storeId={}", userId, storeId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerResponse> getPartners(String query, int limit) {
    int max = clampLimit(limit);
    String q = query != null ? query.trim() : "";

    List<User> merchants =
        userRepository.searchByRoleAndQuery(
            UserRole.MERCHANT, q, PageRequest.of(0, max, Sort.by("id").ascending()));

    if (merchants.isEmpty()) {
      return List.of();
    }

    List<Long> merchantIds = merchants.stream().map(User::getId).toList();
    Map<Long, Integer> storeCounts = fetchStoreCounts(merchantIds);

    return merchants.stream()
        .map(u -> mapToPartnerResponse(u, storeCounts.getOrDefault(u.getId(), 0)))
        .toList();
  }

  private int clampLimit(int limit) {
    return (limit > 0 && limit <= MAX_LIMIT) ? limit : DEFAULT_LIMIT;
  }

  private Map<Long, Integer> fetchStoreCounts(List<Long> merchantIds) {
    Map<Long, Integer> storeCounts = new HashMap<>();
    for (Object[] row : userStoreRepository.countStoresByUserIds(merchantIds)) {
      if (row != null
          && row.length >= 2
          && row[0] instanceof Number uId
          && row[1] instanceof Number count) {
        storeCounts.put(uId.longValue(), count.intValue());
      }
    }
    return storeCounts;
  }

  private PartnerResponse mapToPartnerResponse(User u, int storeCount) {
    String fullName = (u.getFirstName() + " " + u.getLastName()).trim();
    String companyName =
        (u.getPickupLocation() != null && !u.getPickupLocation().isBlank())
            ? u.getPickupLocation()
            : "Boutique " + u.getLastName();
    return PartnerResponse.builder()
        .id(u.getId())
        .publicId(u.getPublicId())
        .name(fullName)
        .email(u.getEmail())
        .companyName(companyName)
        .role(u.getRole().name())
        .existingStoresCount(storeCount)
        .build();
  }

  @Override
  @Transactional
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
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader, userEmailHeader);
    return getVehicles(user.getPublicId());
  }

  @Override
  @Transactional
  public VehicleResponse addCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, VehicleRequest request) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader, userEmailHeader);
    return addVehicle(user.getPublicId(), request);
  }

  @Override
  @Transactional
  public VehicleResponse updateCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, String vehicleId, VehicleRequest request) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader, userEmailHeader);
    return updateVehicle(user.getPublicId(), vehicleId, request);
  }

  @Override
  @Transactional
  public void removeCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, String vehicleId) {
    User user = UserIdentityResolver.resolveUser(userRepository, userIdHeader, userEmailHeader);
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
