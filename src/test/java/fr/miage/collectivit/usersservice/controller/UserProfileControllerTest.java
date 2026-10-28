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
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.exception.VehicleAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.VehicleNotFoundException;
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
}
