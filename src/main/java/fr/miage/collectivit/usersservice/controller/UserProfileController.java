package fr.miage.collectivit.usersservice.controller;

import fr.miage.collectivit.usersservice.config.OpenApiConfig;
import fr.miage.collectivit.usersservice.dto.PartnerResponse;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserStoreLinkRequest;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.http.MediaType;
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
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class UserProfileController {

  private static final String OK_STATUS = "200";
  private static final String CREATED_STATUS = "201";
  private static final String NO_CONTENT_STATUS = "204";
  private static final String BAD_REQUEST_STATUS = "400";
  private static final String UNAUTHORIZED_STATUS = "401";
  private static final String FORBIDDEN_STATUS = "403";
  private static final String NOT_FOUND_STATUS = "404";
  private static final String CONFLICT_STATUS = "409";
  private static final String USER_NOT_FOUND_MSG = "User profile not found";
  private static final String UNAUTHORIZED_MSG =
      "Unauthorized - Missing authentication headers or token";
  private static final String ACCESS_DENIED_MSG = "Access denied";
  private static final String INVALID_PLATE_MSG = "Invalid license plate";
  private static final String USER_OR_VEHICLE_NOT_FOUND_MSG = "User or vehicle not found";
  private static final String HEADER_USER_ID = "X-User-Id";
  private static final String HEADER_USER_EMAIL = "X-User-Email";
  private static final String ROLE_ADMIN = "ADMIN";

  private final UserProfileService userProfileService;

  @GetMapping("/me")
  @Operation(
      summary = "Get current user profile",
      description =
          "Retrieves the authenticated user's profile using identity headers or JWT session",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Current user profile retrieved",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = UNAUTHORIZED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<UserResponse> getCurrentUserProfile(
      @Parameter(
              description = "UUID identifier forwarded by the gateway",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
          @RequestHeader(value = HEADER_USER_ID, required = false)
          String userIdHeader,
      @Parameter(
              description = "Email address forwarded by the gateway",
              example = "alice.dupont@example.com")
          @RequestHeader(value = HEADER_USER_EMAIL, required = false)
          String userEmailHeader,
      HttpServletRequest servletRequest) {
    String effectiveUserId = resolveAuthId(userIdHeader, servletRequest);
    String effectiveUserEmail = resolveAuthEmail(userEmailHeader, servletRequest);
    return ResponseEntity.ok(
        userProfileService.getCurrentUserProfile(effectiveUserId, effectiveUserEmail));
  }

  private String resolveAuthId(String headerId, HttpServletRequest servletRequest) {
    if (headerId != null && !headerId.isBlank()) {
      return headerId;
    }
    return servletRequest != null
        ? (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_ID)
        : null;
  }

  private String resolveAuthEmail(String headerEmail, HttpServletRequest servletRequest) {
    if (headerEmail != null && !headerEmail.isBlank()) {
      return headerEmail;
    }
    return servletRequest != null
        ? (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_EMAIL)
        : null;
  }

  public ResponseEntity<UserResponse> getCurrentUserProfile(
      String userIdHeader, String userEmailHeader) {
    return getCurrentUserProfile(userIdHeader, userEmailHeader, null);
  }

  @GetMapping("/partners")
  @Operation(
      summary = "List partner merchants",
      description = "Retrieves active registered partners for store linking",
      security = {})
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "List of partners matching criteria",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = PartnerResponse.class))))
  })
  public ResponseEntity<List<PartnerResponse>> getPartners(
      @Parameter(description = "Textual filter on merchant name or brand", example = "boulangerie")
          @RequestParam(value = "query", required = false)
          String query,
      @Parameter(description = "Maximum number of results to return", example = "20")
          @RequestParam(value = "limit", required = false, defaultValue = "20")
          int limit) {
    return ResponseEntity.ok(userProfileService.getPartners(query, limit));
  }

  @GetMapping("/{userId}")
  @Operation(
      summary = "Get user profile",
      description = "Retrieves user profile details by public user UUID or internal numeric ID",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "User profile found",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Access denied: cannot view another user's personal profile",
        content = @Content)
  })
  public ResponseEntity<UserResponse> getUserProfile(
      @Parameter(
              description = "Public UUID identifier or numeric ID of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          String userId,
      HttpServletRequest servletRequest) {
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
          "Access denied: you are not authorized to view this user's personal details.");
    }
  }

  private void checkUserAccess(UUID targetUserId, HttpServletRequest servletRequest) {
    if (servletRequest == null || targetUserId == null) {
      return;
    }
    String role = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);
    if (ROLE_ADMIN.equalsIgnoreCase(role)) {
      return;
    }

    String authUserId = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_ID);
    String authEmail = (String) servletRequest.getAttribute(JwtAuthInterceptor.ATTR_USER_EMAIL);
    if (isUnauthenticated(role, authUserId, authEmail)) {
      return;
    }

    if (isTargetUser(targetUserId, authUserId, authEmail)) {
      return;
    }

    throw new ForbiddenException(
        "Access denied: you are not authorized to manage this user's vehicles.");
  }

  private boolean isUnauthenticated(String role, String authUserId, String authEmail) {
    return role == null && authUserId == null && authEmail == null;
  }

  private boolean isTargetUser(UUID targetUserId, String authUserId, String authEmail) {
    return (authUserId != null && authUserId.equalsIgnoreCase(targetUserId.toString()))
        || (authEmail != null && isUserEmailMatching(targetUserId, authEmail));
  }

  private boolean isUserEmailMatching(UUID targetUserId, String authEmail) {
    try {
      UserResponse userResponse = userProfileService.getUserProfile(targetUserId);
      return userResponse != null && authEmail.equalsIgnoreCase(userResponse.email());
    } catch (UserNotFoundException ignored) {
      return false;
    }
  }

  @PostMapping("/stores")
  @Operation(
      summary = "Link store to user",
      description = "Records a relationship between a partner user and a store",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Store successfully linked to user",
        content = @Content),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = "Invalid request payload",
        content = @Content)
  })
  public ResponseEntity<Void> linkStoreToUser(
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Link payload between partner and store",
              required = true)
          @RequestBody
          UserStoreLinkRequest request) {
    userProfileService.linkStoreToUser(request.userId(), request.storeId());
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{userId}")
  @Operation(
      summary = "Update user profile",
      description = "Updates personal details for the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "User profile updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = "Invalid request payload",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<UserResponse> updateUserProfile(
      @Parameter(
              description = "UUID identifier of the user to update",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          UUID userId,
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Updated profile data",
              required = true)
          @RequestBody
          UserUpdateRequest request) {
    return ResponseEntity.ok(userProfileService.updateUserProfile(userId, request));
  }

  @DeleteMapping("/{userId}")
  @Operation(
      summary = "Delete user profile",
      description = "Deletes user profile by public user ID",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = NO_CONTENT_STATUS,
        description = "User profile successfully deleted",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<Void> deleteUserProfile(
      @Parameter(
              description = "UUID identifier of the user to delete",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          UUID userId) {
    userProfileService.deleteUserProfile(userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{userId}/vehicles")
  @Operation(
      summary = "Get user vehicles",
      description = "Retrieves the list of registered vehicles for a user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "List of vehicles",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = VehicleResponse.class)))),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = ACCESS_DENIED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<List<VehicleResponse>> getVehicles(
      @Parameter(
              description = "UUID identifier of the owner user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          UUID userId,
      HttpServletRequest servletRequest) {
    checkUserAccess(userId, servletRequest);
    return ResponseEntity.ok(userProfileService.getVehicles(userId));
  }

  @PostMapping("/{userId}/vehicles")
  @Operation(
      summary = "Add vehicle",
      description = "Associates a new license plate to the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = CREATED_STATUS,
        description = "Vehicle successfully added",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = VehicleResponse.class))),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = INVALID_PLATE_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = ACCESS_DENIED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = CONFLICT_STATUS,
        description = "Vehicle license plate already registered for this user",
        content = @Content)
  })
  public ResponseEntity<VehicleResponse> addVehicle(
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          UUID userId,
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Vehicle information to add",
              required = true)
          @RequestBody
          VehicleRequest request,
      HttpServletRequest servletRequest) {
    checkUserAccess(userId, servletRequest);
    VehicleResponse response = userProfileService.addVehicle(userId, request);
    URI location = URI.create("/v1/users/" + userId + "/vehicles/" + response.licensePlate());
    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/{userId}/vehicles/{vehicleId}")
  @Operation(
      summary = "Update vehicle",
      description = "Updates an existing license plate for the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Vehicle successfully updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = VehicleResponse.class))),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = INVALID_PLATE_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = ACCESS_DENIED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_OR_VEHICLE_NOT_FOUND_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = CONFLICT_STATUS,
        description = "New vehicle license plate already registered for this user",
        content = @Content)
  })
  public ResponseEntity<VehicleResponse> updateVehicle(
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          UUID userId,
      @Parameter(
              description = "Vehicle identifier or previous license plate",
              example = "AA-123-BB",
              required = true)
          @PathVariable
          String vehicleId,
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "New license plate details",
              required = true)
          @RequestBody
          VehicleRequest request,
      HttpServletRequest servletRequest) {
    checkUserAccess(userId, servletRequest);
    return ResponseEntity.ok(userProfileService.updateVehicle(userId, vehicleId, request));
  }

  @DeleteMapping("/{userId}/vehicles/{vehicleId}")
  @Operation(
      summary = "Remove vehicle",
      description = "Disassociates a vehicle from the specified user",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = NO_CONTENT_STATUS,
        description = "Vehicle successfully removed",
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = ACCESS_DENIED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_OR_VEHICLE_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<Void> removeVehicle(
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              required = true)
          @PathVariable
          UUID userId,
      @Parameter(
              description = "Vehicle identifier or license plate to delete",
              example = "AA-123-BB",
              required = true)
          @PathVariable
          String vehicleId,
      HttpServletRequest servletRequest) {
    checkUserAccess(userId, servletRequest);
    userProfileService.removeVehicle(userId, vehicleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me/vehicles")
  @Operation(
      summary = "Get current user vehicles",
      description = "Retrieves the list of registered vehicles for the authenticated client",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "List of vehicles",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = VehicleResponse.class)))),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = UNAUTHORIZED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Only clients can register vehicles",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<List<VehicleResponse>> getCurrentUserVehicles(
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
          @RequestHeader(value = HEADER_USER_ID, required = false)
          String userIdHeader,
      @Parameter(description = "Email address of the user", example = "alice.dupont@example.com")
          @RequestHeader(value = HEADER_USER_EMAIL, required = false)
          String userEmailHeader,
      HttpServletRequest servletRequest) {
    String authUserId = resolveAuthId(userIdHeader, servletRequest);
    String authEmail = resolveAuthEmail(userEmailHeader, servletRequest);
    return ResponseEntity.ok(userProfileService.getCurrentUserVehicles(authUserId, authEmail));
  }

  @PostMapping("/me/vehicles")
  @Operation(
      summary = "Add vehicle for current user",
      description = "Associates a new license plate to the authenticated client",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = CREATED_STATUS,
        description = "Vehicle successfully added",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = VehicleResponse.class))),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = INVALID_PLATE_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = UNAUTHORIZED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Only clients can register vehicles",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_NOT_FOUND_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = CONFLICT_STATUS,
        description = "Vehicle license plate already registered for this user",
        content = @Content)
  })
  public ResponseEntity<VehicleResponse> addCurrentUserVehicle(
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
          @RequestHeader(value = HEADER_USER_ID, required = false)
          String userIdHeader,
      @Parameter(description = "Email address of the user", example = "alice.dupont@example.com")
          @RequestHeader(value = HEADER_USER_EMAIL, required = false)
          String userEmailHeader,
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Vehicle information to register",
              required = true)
          @RequestBody
          VehicleRequest request,
      HttpServletRequest servletRequest) {
    String authUserId = resolveAuthId(userIdHeader, servletRequest);
    String authEmail = resolveAuthEmail(userEmailHeader, servletRequest);
    VehicleResponse response =
        userProfileService.addCurrentUserVehicle(authUserId, authEmail, request);
    URI location = URI.create("/v1/users/me/vehicles/" + response.licensePlate());
    return ResponseEntity.created(location).body(response);
  }

  @PutMapping("/me/vehicles/{vehicleId}")
  @Operation(
      summary = "Update vehicle for current user",
      description = "Updates an existing license plate for the authenticated client",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = OK_STATUS,
        description = "Vehicle successfully updated",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = VehicleResponse.class))),
    @ApiResponse(
        responseCode = BAD_REQUEST_STATUS,
        description = INVALID_PLATE_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = UNAUTHORIZED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Only clients can modify vehicles",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_OR_VEHICLE_NOT_FOUND_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = CONFLICT_STATUS,
        description = "New vehicle license plate already registered for this user",
        content = @Content)
  })
  public ResponseEntity<VehicleResponse> updateCurrentUserVehicle(
      @Parameter(
              description = "Current vehicle identifier or license plate",
              example = "AA-123-BB",
              required = true)
          @PathVariable
          String vehicleId,
      @Valid
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Nouvelle plaque d'immatriculation",
              required = true)
          @RequestBody
          VehicleRequest request,
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
          @RequestHeader(value = HEADER_USER_ID, required = false)
          String userIdHeader,
      @Parameter(description = "Email address of the user", example = "alice.dupont@example.com")
          @RequestHeader(value = HEADER_USER_EMAIL, required = false)
          String userEmailHeader,
      HttpServletRequest servletRequest) {
    String authUserId = resolveAuthId(userIdHeader, servletRequest);
    String authEmail = resolveAuthEmail(userEmailHeader, servletRequest);
    return ResponseEntity.ok(
        userProfileService.updateCurrentUserVehicle(authUserId, authEmail, vehicleId, request));
  }

  @DeleteMapping("/me/vehicles/{vehicleId}")
  @Operation(
      summary = "Remove vehicle for current user",
      description = "Disassociates a vehicle from the authenticated client",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses({
    @ApiResponse(
        responseCode = NO_CONTENT_STATUS,
        description = "Vehicle successfully removed",
        content = @Content),
    @ApiResponse(
        responseCode = UNAUTHORIZED_STATUS,
        description = UNAUTHORIZED_MSG,
        content = @Content),
    @ApiResponse(
        responseCode = FORBIDDEN_STATUS,
        description = "Forbidden - Only clients can remove vehicles",
        content = @Content),
    @ApiResponse(
        responseCode = NOT_FOUND_STATUS,
        description = USER_OR_VEHICLE_NOT_FOUND_MSG,
        content = @Content)
  })
  public ResponseEntity<Void> removeCurrentUserVehicle(
      @Parameter(
              description = "Vehicle identifier or license plate to delete",
              example = "AA-123-BB",
              required = true)
          @PathVariable
          String vehicleId,
      @Parameter(
              description = "UUID identifier of the user",
              example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
          @RequestHeader(value = HEADER_USER_ID, required = false)
          String userIdHeader,
      @Parameter(description = "Email address of the user", example = "alice.dupont@example.com")
          @RequestHeader(value = HEADER_USER_EMAIL, required = false)
          String userEmailHeader,
      HttpServletRequest servletRequest) {
    String authUserId = resolveAuthId(userIdHeader, servletRequest);
    String authEmail = resolveAuthEmail(userEmailHeader, servletRequest);
    userProfileService.removeCurrentUserVehicle(authUserId, authEmail, vehicleId);
    return ResponseEntity.noContent().build();
  }
}
