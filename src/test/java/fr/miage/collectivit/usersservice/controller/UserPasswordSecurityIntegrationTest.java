package fr.miage.collectivit.usersservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class UserPasswordSecurityIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void shouldPersistUserWithBcryptHashedPasswordAndNeverExposePlaintextOrHashInApiResponse()
      throws Exception {
    String rawPassword = "Secur3P@ssw0rd!";
    String rawEmail = "Secure.Client@Example.COM";

    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Alex")
            .lastName("Vidal")
            .birthDate(LocalDate.of(1992, 6, 15))
            .email(rawEmail)
            .password(rawPassword)
            .pickupLocation("Mairie du 15eme")
            .licensePlates(List.of("EF-456-GH"))
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("secure.client@example.com"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.clientCode").isString());

    Optional<User> persisted = userRepository.findByEmail("secure.client@example.com");
    assertThat(persisted).isPresent();

    User userInDb = persisted.get();
    assertThat(userInDb.getPasswordHash())
        .isNotBlank()
        .isNotEqualTo(rawPassword)
        .startsWith("$2a$12$");

    assertThat(passwordEncoder.matches(rawPassword, userInDb.getPasswordHash())).isTrue();
    assertThat(passwordEncoder.matches("WrongPassword123!", userInDb.getPasswordHash())).isFalse();
    assertThat(userInDb.getEmail()).isEqualTo("secure.client@example.com");
  }

  @Test
  void shouldEnforceEmailCaseInsensitiveUniqueness() throws Exception {
    UserRegistrationRequest firstRequest =
        UserRegistrationRequest.builder()
            .firstName("First")
            .lastName("User")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("case.unique@example.com")
            .password("ValidP@ss1!")
            .pickupLocation("Mairie Centrale")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isCreated());

    UserRegistrationRequest duplicateCaseRequest =
        UserRegistrationRequest.builder()
            .firstName("Second")
            .lastName("User")
            .birthDate(LocalDate.of(1991, 2, 2))
            .email("CASE.UNIQUE@EXAMPLE.COM")
            .password("ValidP@ss2!")
            .pickupLocation("Commerce Partenaire A")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateCaseRequest)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value("A user with email 'case.unique@example.com' already exists"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "short1!", // less than 8 chars
        "alllowercase1!", // missing uppercase
        "ALLUPPERCASE1!", // missing lowercase
        "NoDigitsHere!", // missing digit
        "NoSpecialChar12" // missing special character
      })
  void shouldRejectWeakPasswordsViolatingComplexityPolicy(String weakPassword) throws Exception {
    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Marc")
            .lastName("Test")
            .birthDate(LocalDate.of(1995, 3, 10))
            .email("marc.test@example.com")
            .password(weakPassword)
            .pickupLocation("Mairie")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.invalid_params.password").exists());
  }

  // Physical card pickup location is optional as the customer can choose not to request a physical
  // card
  @Test
  void shouldAllowRegistrationWithoutPickupLocationWhenCustomerOptsOut() throws Exception {
    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Marc")
            .lastName("Test")
            .birthDate(LocalDate.of(1995, 3, 10))
            .email("marc.noloc@example.com")
            .password("Str0ngP@ss1!")
            .pickupLocation(null)
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.pickupLocation").doesNotExist());

    Optional<User> persisted = userRepository.findByEmail("marc.noloc@example.com");
    assertThat(persisted).isPresent();
    assertThat(persisted.get().getPickupLocation()).isNull();
  }

  @Test
  void shouldRejectRegistrationWhenUserIsUnder18YearsOld() throws Exception {
    UserRegistrationRequest minorRequest =
        UserRegistrationRequest.builder()
            .firstName("Kevin")
            .lastName("Junior")
            .birthDate(LocalDate.now().minusYears(16))
            .email("kevin.junior@example.com")
            .password("ValidP@ss123!")
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minorRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.invalid_params.birthDate").value("User must be at least 18 years old"));

    assertThat(userRepository.findByEmail("kevin.junior@example.com")).isEmpty();
  }

  @Test
  void shouldNormalizeFrenchLicensePlatesAndRejectInvalidOnes() throws Exception {
    UserRegistrationRequest request =
        UserRegistrationRequest.builder()
            .firstName("Paul")
            .lastName("Galtier")
            .birthDate(LocalDate.of(1991, 5, 2))
            .email("paul.plates@example.com")
            .password("Str0ngP@ss1!")
            .licensePlates(List.of("aa 123 aa", "1234ab75"))
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.licensePlates[0]").value("AA-123-AA"))
        .andExpect(jsonPath("$.licensePlates[1]").value("1234 AB 75"));

    Optional<User> userInDb = userRepository.findByEmail("paul.plates@example.com");
    assertThat(userInDb).isPresent();
    assertThat(userInDb.get().getLicensePlates()).containsExactly("AA-123-AA", "1234 AB 75");
  }
}
