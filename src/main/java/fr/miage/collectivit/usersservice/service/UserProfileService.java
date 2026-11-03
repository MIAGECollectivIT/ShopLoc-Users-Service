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

  UserResponse getUserProfileByIdentifier(String identifier);

  void linkStoreToUser(Long userId, Long storeId);

  List<fr.miage.collectivit.usersservice.dto.PartnerResponse> getPartners(String query, int limit);

  UserResponse updateUserProfile(UUID userId, UserUpdateRequest request);

  void deleteUserProfile(UUID userId);

  List<VehicleResponse> getVehicles(UUID userId);

  VehicleResponse addVehicle(UUID userId, VehicleRequest request);

  VehicleResponse updateVehicle(UUID userId, String vehicleId, VehicleRequest request);

  void removeVehicle(UUID userId, String vehicleId);

  List<VehicleResponse> getCurrentUserVehicles(String userIdHeader, String userEmailHeader);

  VehicleResponse addCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, VehicleRequest request);

  VehicleResponse updateCurrentUserVehicle(
      String userIdHeader, String userEmailHeader, String vehicleId, VehicleRequest request);

  void removeCurrentUserVehicle(String userIdHeader, String userEmailHeader, String vehicleId);
}
