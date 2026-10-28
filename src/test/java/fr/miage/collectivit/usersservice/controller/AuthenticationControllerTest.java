package fr.miage.collectivit.usersservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.EmailAlreadyExistsException;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.service.AuthenticationService;
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
class AuthenticationControllerTest {

  @Mock private AuthenticationService authenticationService;

  @InjectMocks private AuthenticationController authenticationController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(authenticationController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Test
  void shouldRegisterUserAndReturnCreated() throws Exception {
    UUID userId = UUID.randomUUID();
    UserResponse response =
        UserResponse.builder()
            .id(userId)
            .email("client@test.com")
            .role(UserRole.CLIENT)
            .firstName("Luc")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1990, 1, 1))
            .clientCode("aB3dE7K")
            .pickupLocation("Mairie")
            .licensePlates(List.of("AA-123-AA"))
            .build();

    when(authenticationService.register(any())).thenReturn(response);

    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Luc")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("client@test.com")
            .password("P@ssw0rd123!")
            .pickupLocation("Mairie")
            .licensePlates(List.of("AA-123-AA"))
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/v1/users/" + userId))
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("client@test.com"))
        .andExpect(jsonPath("$.clientCode").value("aB3dE7K"));
  }

  @Test
  void shouldRegisterUserWithoutPickupLocationAndReturnCreated() throws Exception {
    UUID userId = UUID.randomUUID();
    UserResponse response =
        UserResponse.builder()
            .id(userId)
            .email("digital.only@test.com")
            .role(UserRole.CLIENT)
            .firstName("Julie")
            .lastName("Roux")
            .birthDate(LocalDate.of(1995, 5, 20))
            .clientCode("jR55kL2")
            .pickupLocation(null)
            .licensePlates(List.of())
            .build();

    when(authenticationService.register(any())).thenReturn(response);

    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Julie")
            .lastName("Roux")
            .birthDate(LocalDate.of(1995, 5, 20))
            .email("digital.only@test.com")
            .password("P@ssw0rd123!")
            .pickupLocation(null)
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/v1/users/" + userId))
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.pickupLocation").doesNotExist());
  }

  @Test
  void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
    when(authenticationService.register(any()))
        .thenThrow(new EmailAlreadyExistsException("existing@test.com"));

    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Luc")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("existing@test.com")
            .password("P@ssw0rd123!")
            .pickupLocation("Mairie")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value("A user with email 'existing@test.com' already exists"));
  }

  @Test
  void shouldReturnBadRequestWhenRequiredFieldsAreMissing() throws Exception {
    UserRegistrationRequest invalidRequest =
        UserRegistrationRequest.builder()
            .firstName("")
            .lastName("")
            .email("invalid-email")
            .password("short")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.invalid_params").exists());
  }

  @Test
  void shouldReturnBadRequestWhenUserIsUnder18() throws Exception {
    UserRegistrationRequest minorRequest =
        UserRegistrationRequest.builder()
            .firstName("Tom")
            .lastName("Sawyer")
            .birthDate(LocalDate.now().minusYears(17))
            .email("tom.minor@example.com")
            .password("Str0ngP@ss1!")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minorRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.invalid_params.birthDate").value("User must be at least 18 years old"));
  }

  @Test
  void shouldReturnBadRequestWhenLicensePlateIsInvalid() throws Exception {
    UserRegistrationRequest invalidPlateRequest =
        UserRegistrationRequest.builder()
            .firstName("Marc")
            .lastName("Vidal")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("marc.plate@example.com")
            .password("Str0ngP@ss1!")
            .licensePlates(List.of("AA-123-OO"))
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPlateRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.invalid_params['licensePlates[0]']").exists());
  }
}
