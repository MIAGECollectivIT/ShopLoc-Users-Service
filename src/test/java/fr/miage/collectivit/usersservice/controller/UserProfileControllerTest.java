package fr.miage.collectivit.usersservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.dto.UserUpdateRequest;
import fr.miage.collectivit.usersservice.dto.VehicleRequest;
import fr.miage.collectivit.usersservice.dto.VehicleResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.ForbiddenException;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.exception.VehicleAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.VehicleNotFoundException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.UserProfileService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

  private static final String NOT_FOUND_STATUS = "404";
  private static final String USER_NOT_FOUND_MSG = "User not found";

  @Mock private UserProfileService userProfileService;

  @InjectMocks private UserProfileController userProfileController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private UUID userId;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(userProfileController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper().findAndRegisterModules();
    userId = UUID.randomUUID();
  }

  @Test
  void shouldGetCurrentUserProfileWithHeader() throws Exception {
    UserResponse response =
        UserResponse.builder()
            .id(userId)
            .email("client@test.com")
            .role(UserRole.CLIENT)
            .firstName("Sophie")
            .lastName("Martin")
            .birthDate(LocalDate.of(1988, 4, 12))
            .clientCode("sM88kL9")
            .pickupLocation("Mairie")
            .licensePlates(List.of("AA-123-AA"))
            .build();

    when(userProfileService.getCurrentUserProfile(userId.toString(), null)).thenReturn(response);

    mockMvc
        .perform(get("/v1/users/me").header("X-User-Id", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("client@test.com"))
        .andExpect(jsonPath("$.firstName").value("Sophie"));
  }

  @Test
  void shouldGetCurrentUserProfileOnApiV1EndpointForClient() throws Exception {
    UserResponse response =
        UserResponse.builder()
            .id(userId)
            .email("client@test.com")
            .role(UserRole.CLIENT)
            .firstName("Sophie")
            .lastName("Martin")
            .birthDate(LocalDate.of(1988, 4, 12))
            .clientCode("CL12345")
            .pickupLocation("Mairie")
            .licensePlates(List.of("AA-123-AA", "BB-456-BB"))
            .build();

    when(userProfileService.getCurrentUserProfile(userId.toString(), null)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/me").header("X-User-Id", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("client@test.com"))
        .andExpect(jsonPath("$.role").value("CLIENT"))
        .andExpect(jsonPath("$.firstName").value("Sophie"))
        .andExpect(jsonPath("$.lastName").value("Martin"))
        .andExpect(jsonPath("$.clientCode").value("CL12345"))
        .andExpect(jsonPath("$.pickupLocation").value("Mairie"))
        .andExpect(jsonPath("$.licensePlates[0]").value("AA-123-AA"))
        .andExpect(jsonPath("$.licensePlates[1]").value("BB-456-BB"));
  }

  @Test
  void shouldGetCurrentUserProfileOnApiV1EndpointForMerchant() throws Exception {
    UUID merchantId = UUID.randomUUID();
    UserResponse response =
        UserResponse.builder()
            .id(merchantId)
            .email("merchant@test.com")
            .role(UserRole.MERCHANT)
            .firstName("Pierre")
            .lastName("Boulanger")
            .birthDate(LocalDate.of(1975, 6, 20))
            .clientCode(null)
            .pickupLocation(null)
            .licensePlates(null)
            .build();

    when(userProfileService.getCurrentUserProfile(merchantId.toString(), null))
        .thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/me").header("X-User-Id", merchantId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(merchantId.toString()))
        .andExpect(jsonPath("$.email").value("merchant@test.com"))
        .andExpect(jsonPath("$.role").value("MERCHANT"))
        .andExpect(jsonPath("$.firstName").value("Pierre"))
        .andExpect(jsonPath("$.clientCode").doesNotExist())
        .andExpect(jsonPath("$.licensePlates").doesNotExist())
        .andExpect(jsonPath("$.pickupLocation").doesNotExist());
  }

  @Test
  void shouldGetCurrentUserProfileOnApiV1EndpointForAdmin() throws Exception {
    UUID adminId = UUID.randomUUID();
    UserResponse response =
        UserResponse.builder()
            .id(adminId)
            .email("admin@test.com")
            .role(UserRole.ADMIN)
            .firstName("Admin")
            .lastName("ShopLoc")
            .birthDate(LocalDate.of(1980, 1, 1))
            .clientCode(null)
            .pickupLocation(null)
            .licensePlates(null)
            .build();

    when(userProfileService.getCurrentUserProfile(adminId.toString(), null)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/me").header("X-User-Id", adminId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(adminId.toString()))
        .andExpect(jsonPath("$.email").value("admin@test.com"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.clientCode").doesNotExist())
        .andExpect(jsonPath("$.licensePlates").doesNotExist())
        .andExpect(jsonPath("$.pickupLocation").doesNotExist());
  }

  @Test
  void shouldReturnUnauthorizedWhenMeCalledWithoutHeaders() throws Exception {
    when(userProfileService.getCurrentUserProfile(null, null))
        .thenThrow(
            new fr.miage.collectivit.usersservice.exception.UnauthorizedException(
                "Missing authentication identity headers"));

    mockMvc
        .perform(get("/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Missing authentication identity headers"));
  }

  @Test
  void shouldGetUserProfile() throws Exception {
    UserResponse response =
        UserResponse.builder()
            .id(userId)
            .email("client@test.com")
            .role(UserRole.CLIENT)
            .firstName("Sophie")
            .lastName("Martin")
            .birthDate(LocalDate.of(1988, 4, 12))
            .clientCode("sM88kL9")
            .pickupLocation("Mairie")
            .licensePlates(List.of("AA-123-AA"))
            .build();

    when(userProfileService.getUserProfile(userId)).thenReturn(response);

    mockMvc
        .perform(get("/v1/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.firstName").value("Sophie"));
  }

  @Test
  void shouldReturnNotFoundWhenUserDoesNotExistOnGet() throws Exception {
    when(userProfileService.getUserProfile(userId)).thenThrow(new UserNotFoundException(userId));

    mockMvc
        .perform(get("/v1/users/{userId}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("User not found with id: " + userId));
  }

  @Test
  void shouldUpdateUserProfile() throws Exception {
    UserUpdateRequest updateRequest =
        UserUpdateRequest.builder()
            .firstName("Sophie-Anne")
            .lastName("Martin-Dupont")
            .birthDate(LocalDate.of(1988, 4, 12))
            .pickupLocation("Mairie Annexe")
            .build();

    UserResponse response =
        UserResponse.builder()
            .id(userId)
            .email("client@test.com")
            .role(UserRole.CLIENT)
            .firstName("Sophie-Anne")
            .lastName("Martin-Dupont")
            .birthDate(LocalDate.of(1988, 4, 12))
            .clientCode("sM88kL9")
            .pickupLocation("Mairie Annexe")
            .build();

    when(userProfileService.updateUserProfile(eq(userId), any())).thenReturn(response);

    mockMvc
        .perform(
            put("/v1/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Sophie-Anne"))
        .andExpect(jsonPath("$.pickupLocation").value("Mairie Annexe"));
  }

  @Test
  void shouldDeleteUserProfile() throws Exception {
    doNothing().when(userProfileService).deleteUserProfile(userId);

    mockMvc.perform(delete("/v1/users/{userId}", userId)).andExpect(status().isNoContent());
  }

  @Test
  void shouldGetVehicles() throws Exception {
    List<VehicleResponse> vehicles =
        List.of(
            VehicleResponse.fromLicensePlate("AA-123-AA"),
            VehicleResponse.fromLicensePlate("BB-456-BB"));
    when(userProfileService.getVehicles(userId)).thenReturn(vehicles);

    mockMvc
        .perform(get("/v1/users/{userId}/vehicles", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].licensePlate").value("AA-123-AA"));
  }

  @Test
  void shouldAddVehicle() throws Exception {
    VehicleRequest request = new VehicleRequest("cc-789-dd");
    VehicleResponse response = VehicleResponse.fromLicensePlate("CC-789-DD");

    when(userProfileService.addVehicle(eq(userId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/v1/users/{userId}/vehicles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/v1/users/" + userId + "/vehicles/CC-789-DD"))
        .andExpect(jsonPath("$.licensePlate").value("CC-789-DD"));
  }

  @Test
  void shouldReturnConflictWhenVehicleAlreadyRegistered() throws Exception {
    VehicleRequest request = new VehicleRequest("AA-123-AA");
    when(userProfileService.addVehicle(eq(userId), any()))
        .thenThrow(new VehicleAlreadyExistsException("AA-123-AA"));

    mockMvc
        .perform(
            post("/v1/users/{userId}/vehicles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldReturnBadRequestWhenAddingInvalidLicensePlate() throws Exception {
    VehicleRequest request = new VehicleRequest("INVALID-PLATE");

    mockMvc
        .perform(
            post("/v1/users/{userId}/vehicles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.invalid_params.licensePlate").exists());
  }

  @Test
  void shouldRemoveVehicle() throws Exception {
    doNothing().when(userProfileService).removeVehicle(userId, "AA-123-AA");

    mockMvc
        .perform(delete("/v1/users/{userId}/vehicles/{vehicleId}", userId, "AA-123-AA"))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNotFoundWhenVehicleNotFoundOnRemove() throws Exception {
    doThrow(new VehicleNotFoundException("ZZ-999-ZZ"))
        .when(userProfileService)
        .removeVehicle(userId, "ZZ-999-ZZ");

    mockMvc
        .perform(delete("/v1/users/{userId}/vehicles/{vehicleId}", userId, "ZZ-999-ZZ"))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldDenyAccessWhenDifferentUserRequestsVehicles() throws Exception {
    UUID otherUserId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/v1/users/{userId}/vehicles", userId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ID, otherUserId.toString())
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowAdminAccessingOtherUserVehicles() throws Exception {
    UUID adminId = UUID.randomUUID();
    when(userProfileService.getVehicles(userId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/v1/users/{userId}/vehicles", userId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ID, adminId.toString())
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldAllowSelfAccessingOwnVehicles() throws Exception {
    when(userProfileService.getVehicles(userId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/v1/users/{userId}/vehicles", userId)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ID, userId.toString())
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT"))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnForbiddenWhenNonClientAddsVehicle() throws Exception {
    VehicleRequest request = new VehicleRequest("AA-123-AA");
    when(userProfileService.addVehicle(eq(userId), any()))
        .thenThrow(new ForbiddenException("Seuls les clients peuvent enregistrer un véhicule."));

    mockMvc
        .perform(
            post("/v1/users/{userId}/vehicles", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}