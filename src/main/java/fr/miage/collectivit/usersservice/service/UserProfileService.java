package fr.miage.collectivit.usersservice.service;

import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import java.util.List;
import java.util.UUID;

public interface UserProfileService {

  UserResponse getCurrentUserProfile(String userIdHeader, String userEmailHeader);

  UserResponse getUserProfile(UUID userId);

  UserResponse updateUserProfile(UUID userId, UserUpdateRequest request);

  void deleteUserProfile(UUID userId);

  List<VehicleResponse> getVehicles(UUID userId);

  VehicleResponse addVehicle(UUID userId, VehicleRequest request);

  void removeVehicle(UUID userId, String vehicleId);
}
