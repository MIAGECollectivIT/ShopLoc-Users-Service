package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.exception.VehicleAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.VehicleNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.UserProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserProfile(UUID userId) {
    User user = findUserByPublicId(userId);
    return UserResponse.fromEntity(user);
  }

  @Override
  @Transactional
  public UserResponse updateUserProfile(UUID userId, UserUpdateRequest request) {
    User user = findUserByPublicId(userId);
    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setBirthDate(request.birthDate());
    user.setPickupLocation(
        request.pickupLocation() != null ? request.pickupLocation().trim() : null);

    User savedUser = userRepository.save(user);
    return UserResponse.fromEntity(savedUser);
  }

  @Override
  @Transactional
  public void deleteUserProfile(UUID userId) {
    User user = findUserByPublicId(userId);
    userRepository.delete(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleResponse> getVehicles(UUID userId) {
    User user = findUserByPublicId(userId);
    if (user.getLicensePlates() == null) {
      return List.of();
    }
    return user.getLicensePlates().stream().map(VehicleResponse::fromLicensePlate).toList();
  }

  @Override
  @Transactional
  public VehicleResponse addVehicle(UUID userId, VehicleRequest request) {
    User user = findUserByPublicId(userId);
    String plate = request.licensePlate().trim().toUpperCase(Locale.ROOT);

    if (user.getLicensePlates() == null) {
      user.setLicensePlates(new ArrayList<>());
    }

    boolean alreadyExists =
        user.getLicensePlates().stream().anyMatch(existing -> existing.equalsIgnoreCase(plate));
    if (alreadyExists) {
      throw new VehicleAlreadyExistsException(plate);
    }

    user.getLicensePlates().add(plate);
    userRepository.save(user);
    return VehicleResponse.fromLicensePlate(plate);
  }

  @Override
  @Transactional
  public void removeVehicle(UUID userId, String vehicleId) {
    User user = findUserByPublicId(userId);
    if (user.getLicensePlates() == null || user.getLicensePlates().isEmpty()) {
      throw new VehicleNotFoundException(vehicleId);
    }

    String targetPlate = vehicleId.trim();
    boolean removed =
        user.getLicensePlates().removeIf(plate -> plate.equalsIgnoreCase(targetPlate));
    if (!removed) {
      throw new VehicleNotFoundException(vehicleId);
    }

    userRepository.save(user);
  }

  private User findUserByPublicId(UUID userId) {
    return userRepository
        .findByPublicId(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
  }
}
