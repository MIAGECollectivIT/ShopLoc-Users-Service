package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserStoreLinkRequest;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v1/users", "/api/v1/users"})
@RequiredArgsConstructor
@Tag(
    name = "User Profile",
    description = "Operations related to managing user profiles and vehicles")
@SuppressWarnings("PMD.ExcessiveImports")
public class UserProfileController {

  private static final String OK_STATUS = "200";
  private static final String NOT_FOUND_STATUS = "404";
  private static final String USER_NOT_FOUND_MSG = "User profile not found";
  private static final String ROLE_ADMIN = "ADMIN";

  private final UserProfileService userProfileService;

  @GetMapping("/me")
  @Operation(
      summary = "Get current user profile",
      description =
          "Retrieves the authenticated user's profile using identity headers or JWT session",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = OK_STATUS, description = "Current user profile retrieved"),
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing authentication headers or token"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG)
  })
  public ResponseEntity<UserResponse> getCurrentUserProfile(
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
      @RequestHeader(value = "X-User-Email", required = false) String userEmailHeader,
      HttpServletRequest servletRequest) {
    String effectiveUserId = userIdHeader;
    if ((effectiveUserId == null || effectiveUserId.isBlank()) && servletRequest != null) {
      effectiveUserId = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    }
    String effectiveUserEmail = userEmailHeader;
    if ((effectiveUserEmail == null || effectiveUserEmail.isBlank()) && servletRequest != null) {
      effectiveUserEmail = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_EMAIL);
    }
    return ResponseEntity.ok(
        userProfileService.getCurrentUserProfile(effectiveUserId, effectiveUserEmail));
  }

  public ResponseEntity<UserResponse> getCurrentUserProfile(
      String userIdHeader, String userEmailHeader) {
    return getCurrentUserProfile(userIdHeader, userEmailHeader, null);
  }

  @GetMapping("/partners")
  @Operation(
      summary = "List partner merchants",
      description = "Retrieves active registered partners for store linking")
  public ResponseEntity<List<PartnerResponse>> getPartners(
      @RequestParam(value = "query", required = false) String query,
      @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
    return ResponseEntity.ok(userProfileService.getPartners(query, limit));
  }

  @GetMapping("/{userId}")
  @Operation(
      summary = "Get user profile",
      description = "Retrieves user profile details by public user UUID or internal numeric ID",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = OK_STATUS, description = "User profile found"),
    @ApiResponse(responseCode = NOT_FOUND_STATUS, description = USER_NOT_FOUND_MSG),
    @ApiResponse(
        responseCode = "403",
        description = "Access denied: cannot view another user's personal profile")
  })
  public ResponseEntity<UserResponse> getUserProfile(
      @PathVariable String userId, HttpServletRequest servletRequest) {
    UserResponse userResponse;
    try {
      UUID publicId = UUID.fromString(userId.trim());
      userResponse = userProfileService.getUserProfile(publicId);
    } catch (IllegalArgumentException notUuid) {
      userResponse = userProfileService.getUserProfileByIdentifier(userId);
    }
    checkUserAccess(userResponse, servletRequest);
    return ResponseEntity.ok(userResponse);
  }

  public ResponseEntity<UserResponse> getUserProfile(String userId) {
    return getUserProfile(userId, null);
  }

  private void checkUserAccess(UserResponse userResponse, HttpServletRequest servletRequest) {
    if (servletRequest == null) {
      return;
    }
    String role = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    String authUserId = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    String authEmail = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_EMAIL);

    if (role == null && authUserId == null && authEmail == null) {
      return;
    }

    if (ROLE_ADMIN.equalsIgnoreCase(role)) {
      return;
    }

    boolean isSelf =
        (authUserId != null && authUserId.equalsIgnoreCase(userResponse.id().toString()))
            || (authEmail != null && authEmail.equalsIgnoreCase(userResponse.email()));

    if (!isSelf) {
      throw new ForbiddenException(
          "Accès refusé : vous n'êtes pas autorisé à consulter les données personnelles de cet utilisateur.");
    }
  }

  @PostMapping("/stores")
  @Operation(
      summary = "Link store to user",
      description = "Records a relationship between a partner user and a store",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = OK_STATUS, description = "Store successfully linked to user"),
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
  })
  public ResponseEntity<Void> linkStoreToUser(@Valid @RequestBody UserStoreLinkRequest request) {
    userProfileService.linkStoreToUser(request.userId(), request.storeId());
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{userId}")
  @Operation(
      summary = "Update user profile",
      description = "Updates personal details for the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(responseCode = OK_STATUS, description = "User profile updated"),
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
    @ApiResponse(responseCode = OK_STATUS, description = "List of vehicles"),
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
    URI location = URI.create("/v1/users/" + userId + "/vehicles/" + response.licensePlate());
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
