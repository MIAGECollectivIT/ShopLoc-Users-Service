package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "User Profile",
    description = "Operations related to managing user profiles and vehicles")
public class UserProfileController {

  private static final String NOT_FOUND_STATUS = "404";
  private static final String USER_NOT_FOUND_MSG = "User not found";

  private final UserProfileService userProfileService;

  @GetMapping("/{userId}")
  @Operation(
      summary = "Get user profile",
      description = "Retrieves user profile details by public user ID",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "User profile found"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG)
  })
  public ResponseEntity<UserResponse> getUserProfile(@PathVariable UUID userId) {
    return ResponseEntity.ok(userProfileService.getUserProfile(userId));
  }

  @PutMapping("/{userId}")
  @Operation(
      summary = "Update user profile",
      description = "Updates personal details for the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "User profile updated"),
    @ApiResponse(responseCode = "400", description = "Invalid request payload"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG)
  })
  public ResponseEntity<UserResponse> updateUserProfile(
      @PathVariable UUID userId, @Valid @RequestBody UserUpdateRequest request) {
    return ResponseEntity.ok(userProfileService.updateUserProfile(userId, request));
  }

  @DeleteMapping("/{userId}")
  @Operation(
      summary = "Delete user profile",
      description = "Deletes user profile by public user ID",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "User profile successfully deleted"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG)
  })
  public ResponseEntity<Void> deleteUserProfile(@PathVariable UUID userId) {
    userProfileService.deleteUserProfile(userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{userId}/vehicles")
  @Operation(
      summary = "Get user vehicles",
      description = "Retrieves the list of registered vehicles for a user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "List of vehicles"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG)
  })
  public ResponseEntity<List<VehicleResponse>> getVehicles(@PathVariable UUID userId) {
    return ResponseEntity.ok(userProfileService.getVehicles(userId));
  }

  @PostMapping("/{userId}/vehicles")
  @Operation(
      summary = "Add vehicle",
      description = "Associates a new license plate to the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Vehicle successfully added"),
    @ApiResponse(responseCode = "400", description = "Invalid license plate"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG),
    @ApiResponse(
        responseCode = "409",
        description = "Vehicle license plate already registered for this user")
  })
  public ResponseEntity<VehicleResponse> addVehicle(
      @PathVariable UUID userId, @Valid @RequestBody VehicleRequest request) {
    VehicleResponse response = userProfileService.addVehicle(userId, request);
    URI location = URI.create("/v1/users/" + userId + "/vehicles/" + response.vehicleId());
    return ResponseEntity.created(location).body(response);
  }

  @DeleteMapping("/{userId}/vehicles/{vehicleId}")
  @Operation(
      summary = "Remove vehicle",
      description = "Disassociates a vehicle from the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Vehicle successfully removed"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = "User or vehicle not found")
  })
  public ResponseEntity<Void> removeVehicle(
      @PathVariable UUID userId, @PathVariable String vehicleId) {
    userProfileService.removeVehicle(userId, vehicleId);
    return ResponseEntity.noContent().build();
  }
}
