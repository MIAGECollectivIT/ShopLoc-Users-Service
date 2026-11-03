package fr.miage.collectivit.usersservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.usersservice.dto.LoginRequest;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserStoreLinkRequest;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class UserSecurityIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenService jwtTokenService;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  @DisplayName("GET /v1/users/status est public et retourne 200 sans token")
  void shouldAllowPublicAccessToStatus() throws Exception {
    mockMvc.perform(get("/v1/users/status")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /v1/auth/public-key est public et retourne la clé publique")
  void shouldExposePublicKeyWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/v1/auth/public-key")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /v1/users is disabled and returns 403 Forbidden")
  void shouldRejectPublicRegistrationWhenDisabled() throws Exception {
    UserRegistrationRequest registrationRequest =
        UserRegistrationRequest.builder()
            .firstName("Alice")
            .lastName("Dupond")
            .birthDate(LocalDate.of(1990, 5, 12))
            .email("alice.security@example.com")
            .password("SecurePass123!")
            .pickupLocation("Mairie")
            .licensePlates(List.of())
            .build();

    mockMvc
        .perform(
            post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("Public registration is disabled."));
  }

  @Test
  @DisplayName(
      "POST /v1/users/login valide les identifiants et retourne un token JWT porteur du rôle")
  void shouldAuthenticateAndIssueJwtWithRole() throws Exception {
    User user =
        User.builder()
            .firstName("Bob")
            .lastName("Merchant")
            .birthDate(LocalDate.of(1985, 8, 15))
            .email("bob.merchant@example.com")
            .passwordHash(passwordEncoder.encode("MerchantPass1!"))
            .role(UserRole.MERCHANT)
            .clientCode("CL12345")
            .build();
    userRepository.saveAndFlush(user);

    LoginRequest loginRequest =
        LoginRequest.builder().email("bob.merchant@example.com").password("MerchantPass1!").build();

    mockMvc
        .perform(
            post("/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.role").value("MERCHANT"))
        .andExpect(jsonPath("$.user.email").value("bob.merchant@example.com"));
  }

  @Test
  @DisplayName("POST /v1/users/login avec mauvais mot de passe renvoie 401")
  void shouldRejectInvalidCredentials() throws Exception {
    User user =
        User.builder()
            .firstName("Marc")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1980, 1, 1))
            .email("marc.bad@example.com")
            .passwordHash(passwordEncoder.encode("GoodPassword1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL88888")
            .build();
    userRepository.saveAndFlush(user);

    LoginRequest loginRequest =
        LoginRequest.builder().email("marc.bad@example.com").password("WrongPassword1!").build();

    mockMvc
        .perform(
            post("/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /v1/users/login verrouille le compte après 5 tentatives échouées (HTTP 423)")
  void shouldLockAccountAfterFiveConsecutiveFailedAttempts() throws Exception {
    User user =
        User.builder()
            .firstName("Alice")
            .lastName("Lockout")
            .birthDate(LocalDate.of(1990, 5, 20))
            .email("alice.lockout@example.com")
            .passwordHash(passwordEncoder.encode("GoodPassword1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL99999")
            .build();
    userRepository.saveAndFlush(user);

    LoginRequest badRequest =
        LoginRequest.builder()
            .email("alice.lockout@example.com")
            .password("WrongPassword1!")
            .build();

    for (int i = 0; i < 4; i++) {
      mockMvc
          .perform(
              post("/v1/users/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(badRequest)))
          .andExpect(status().isUnauthorized());
    }

    mockMvc
        .perform(
            post("/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)))
        .andExpect(status().isLocked())
        .andExpect(jsonPath("$.type").value("https://errors.shoploc.com/account-locked"));

    LoginRequest goodRequest =
        LoginRequest.builder()
            .email("alice.lockout@example.com")
            .password("GoodPassword1!")
            .build();
    mockMvc
        .perform(
            post("/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goodRequest)))
        .andExpect(status().isLocked());
  }

  @Test
  @DisplayName("GET /v1/users/me sans token renvoie 401 Unauthorized")
  void shouldDenyMeEndpointWhenTokenMissing() throws Exception {
    mockMvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /v1/users/me avec token Bearer valide renvoie 200 OK")
  void shouldAllowMeEndpointWhenTokenValid() throws Exception {
    User user =
        User.builder()
            .firstName("Leo")
            .lastName("Dupuis")
            .birthDate(LocalDate.of(1995, 3, 20))
            .email("leo.valid@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL55667")
            .build();
    User savedUser = userRepository.saveAndFlush(user);

    String token = jwtTokenService.generateToken(savedUser);

    mockMvc
        .perform(get("/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("leo.valid@example.com"))
        .andExpect(jsonPath("$.role").value("CLIENT"));
  }

  @Test
  @DisplayName("POST /v1/users/logout révoque le jeton et bloque les accès ultérieurs")
  void shouldRevokeTokenOnLogoutAndDenySubsequentRequests() throws Exception {
    User user =
        User.builder()
            .firstName("Claire")
            .lastName("LogoutTest")
            .birthDate(LocalDate.of(1992, 4, 10))
            .email("claire.logout@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL99112")
            .build();
    User savedUser = userRepository.saveAndFlush(user);
    String token = jwtTokenService.generateToken(savedUser);

    // Initial call is authorized
    mockMvc
        .perform(get("/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    // Logout
    mockMvc
        .perform(post("/v1/users/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").exists());

    // Subsequent call with same token is revoked -> 401 Unauthorized
    mockMvc
        .perform(get("/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName(
      "GET /v1/users/{identifier} fonctionne par UUID et par ID numérique pour son propre compte")
  void shouldAllowAccessByNumericIdAndUuidWithToken() throws Exception {
    User user =
        User.builder()
            .firstName("Sophie")
            .lastName("Polymorphic")
            .birthDate(LocalDate.of(1991, 7, 25))
            .email("sophie.poly@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.MERCHANT)
            .clientCode("CL77331")
            .build();
    User savedUser = userRepository.saveAndFlush(user);
    String token = jwtTokenService.generateToken(savedUser);

    // Call by UUID
    mockMvc
        .perform(
            get("/v1/users/" + savedUser.getPublicId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("sophie.poly@example.com"));

    // Call by Long numeric DB id
    mockMvc
        .perform(get("/v1/users/" + savedUser.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("sophie.poly@example.com"));
  }

  @Test
  @DisplayName(
      "GET /v1/users/{userId} pour un autre utilisateur sans rôle ADMIN renvoie 403 Forbidden")
  void shouldDenyAccessToAnotherUserProfileForNonAdminUser() throws Exception {
    User victim =
        User.builder()
            .firstName("Victim")
            .lastName("User")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("victim@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL11223")
            .build();
    User savedVictim = userRepository.saveAndFlush(victim);

    User attacker =
        User.builder()
            .firstName("Attacker")
            .lastName("User")
            .birthDate(LocalDate.of(1995, 2, 2))
            .email("attacker@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL99887")
            .build();
    User savedAttacker = userRepository.saveAndFlush(attacker);
    String attackerToken = jwtTokenService.generateToken(savedAttacker);

    mockMvc
        .perform(
            get("/v1/users/" + savedVictim.getPublicId())
                .header("Authorization", "Bearer " + attackerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /v1/users/{userId} pour un autre utilisateur avec rôle ADMIN renvoie 200 OK")
  void shouldAllowAdminToAccessAnyUserProfile() throws Exception {
    User victim =
        User.builder()
            .firstName("Victim2")
            .lastName("User2")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("victim2@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL33445")
            .build();
    User savedVictim = userRepository.saveAndFlush(victim);

    User admin =
        User.builder()
            .firstName("Admin")
            .lastName("Boss")
            .birthDate(LocalDate.of(1980, 3, 3))
            .email("admin.security@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.ADMIN)
            .clientCode("AD99999")
            .build();
    User savedAdmin = userRepository.saveAndFlush(admin);
    String adminToken = jwtTokenService.generateToken(savedAdmin);

    mockMvc
        .perform(
            get("/v1/users/" + savedVictim.getPublicId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("victim2@example.com"));
  }

  @Test
  @DisplayName("POST /v1/users/stores enregistre le lien entre commerce et utilisateur")
  void shouldAllowStoreLinkEndpointWithToken() throws Exception {
    User user =
        User.builder()
            .firstName("Pierre")
            .lastName("Partner")
            .birthDate(LocalDate.of(1982, 11, 14))
            .email("pierre.partner@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .role(UserRole.MERCHANT)
            .clientCode("CL44556")
            .build();
    User savedUser = userRepository.saveAndFlush(user);
    String token = jwtTokenService.generateToken(savedUser);

    UserStoreLinkRequest request = new UserStoreLinkRequest(savedUser.getId(), 55L);

    mockMvc
        .perform(
            post("/v1/users/stores")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /v1/users/{userId} sans token renvoie 401 Unauthorized")
  void shouldDenyGetUserByIdWithoutToken() throws Exception {
    mockMvc.perform(get("/v1/users/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName(
      "GET /v1/auth/verify avec Cookie shoploc_token retourne 200 et les headers d'identité")
  void shouldAuthenticateViaSessionCookie() throws Exception {
    User user =
        User.builder()
            .firstName("Sophie")
            .lastName("Client")
            .birthDate(LocalDate.of(1995, 4, 20))
            .email("sophie.cookie@example.com")
            .passwordHash(passwordEncoder.encode("CookiePass1!"))
            .role(UserRole.CLIENT)
            .clientCode("CL98765")
            .build();
    User savedUser = userRepository.saveAndFlush(user);
    String token = jwtTokenService.generateToken(savedUser);

    mockMvc
        .perform(
            get("/v1/auth/verify").cookie(new jakarta.servlet.http.Cookie("shoploc_token", token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALID"))
        .andExpect(jsonPath("$.role").value("CLIENT"));
  }
}
