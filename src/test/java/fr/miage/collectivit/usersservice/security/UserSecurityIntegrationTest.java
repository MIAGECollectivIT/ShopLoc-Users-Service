package fr.miage.collectivit.usersservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.usersservice.dto.AdminAddStoreRequest;
import fr.miage.collectivit.usersservice.dto.LoginRequest;
import fr.miage.collectivit.usersservice.dto.UserRegistrationRequest;
import fr.miage.collectivit.usersservice.dto.UserStoreLinkRequest;
import fr.miage.collectivit.usersservice.entity.Admin;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.Merchant;
import fr.miage.collectivit.usersservice.entity.User;
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
@SuppressWarnings("PMD.TooManyMethods")
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
  @DisplayName("GET /v1/users/status is public and returns 200 without token")
  void shouldAllowPublicAccessToStatus() throws Exception {
    mockMvc.perform(get("/v1/users/status")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /v1/auth/public-key is public and returns the public key")
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
      "POST /v1/users/login validates credentials and returns a JWT token carrying the role")
  void shouldAuthenticateAndIssueJwtWithRole() throws Exception {
    Merchant user =
        Merchant.builder()
            .firstName("Bob")
            .lastName("Merchant")
            .birthDate(LocalDate.of(1985, 8, 15))
            .email("bob.merchant@example.com")
            .passwordHash(passwordEncoder.encode("MerchantPass1!"))
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
  @DisplayName("POST /v1/users/login with wrong password returns 401 Unauthorized")
  void shouldRejectInvalidCredentials() throws Exception {
    Client user =
        Client.builder()
            .firstName("Marc")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1980, 1, 1))
            .email("marc.bad@example.com")
            .passwordHash(passwordEncoder.encode("GoodPassword1!"))
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
  @DisplayName("POST /v1/users/login locks account after 5 failed attempts (HTTP 423)")
  void shouldLockAccountAfterFiveConsecutiveFailedAttempts() throws Exception {
    Client user =
        Client.builder()
            .firstName("Alice")
            .lastName("Lockout")
            .birthDate(LocalDate.of(1990, 5, 20))
            .email("alice.lockout@example.com")
            .passwordHash(passwordEncoder.encode("GoodPassword1!"))
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
        .andExpect(status().isLocked())
        .andExpect(jsonPath("$.type").value("https://errors.shoploc.com/account-locked"));
  }

  @Test
  @DisplayName("GET /v1/users/me without token returns 401 Unauthorized")
  void shouldDenyMeEndpointWhenTokenMissing() throws Exception {
    mockMvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /v1/users/me with valid Bearer token returns 200 OK")
  void shouldAllowMeEndpointWhenTokenValid() throws Exception {
    Client user =
        Client.builder()
            .firstName("Leo")
            .lastName("Dupuis")
            .birthDate(LocalDate.of(1995, 3, 20))
            .email("leo.valid@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
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
  @DisplayName(
      "GET /api/v1/users/me with valid Bearer token for Client returns 200 OK with full details")
  void shouldAllowApiMeEndpointWhenClientTokenValid() throws Exception {
    Client user =
        Client.builder()
            .firstName("Emma")
            .lastName("Client")
            .birthDate(LocalDate.of(1996, 7, 14))
            .email("emma.client@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .clientCode("CL11223")
            .pickupLocation("Mairie Centrale")
            .licensePlates(List.of("AA-123-BB"))
            .build();
    User savedUser = userRepository.saveAndFlush(user);

    String token = jwtTokenService.generateToken(savedUser);

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("emma.client@example.com"))
        .andExpect(jsonPath("$.role").value("CLIENT"))
        .andExpect(jsonPath("$.firstName").value("Emma"))
        .andExpect(jsonPath("$.lastName").value("Client"))
        .andExpect(jsonPath("$.clientCode").value("CL11223"))
        .andExpect(jsonPath("$.pickupLocation").value("Mairie Centrale"))
        .andExpect(jsonPath("$.licensePlates[0]").value("AA-123-BB"));
  }

  @Test
  @DisplayName(
      "GET /api/v1/users/me with valid Bearer token for Merchant returns 200 OK without client fields")
  void shouldAllowApiMeEndpointWhenMerchantTokenValidWithoutClientFields() throws Exception {
    Merchant user =
        Merchant.builder()
            .firstName("Hugo")
            .lastName("Marchand")
            .birthDate(LocalDate.of(1985, 3, 10))
            .email("hugo.marchand@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .build();
    User savedUser = userRepository.saveAndFlush(user);

    String token = jwtTokenService.generateToken(savedUser);

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("hugo.marchand@example.com"))
        .andExpect(jsonPath("$.role").value("MERCHANT"))
        .andExpect(jsonPath("$.firstName").value("Hugo"))
        .andExpect(jsonPath("$.clientCode").doesNotExist())
        .andExpect(jsonPath("$.pickupLocation").doesNotExist())
        .andExpect(jsonPath("$.licensePlates").doesNotExist());
  }

  @Test
  @DisplayName(
      "GET /api/v1/users/me with valid Bearer token for Admin returns 200 OK without client fields")
  void shouldAllowApiMeEndpointWhenAdminTokenValidWithoutClientFields() throws Exception {
    Admin user =
        Admin.builder()
            .firstName("Super")
            .lastName("Admin")
            .birthDate(LocalDate.of(1980, 1, 1))
            .email("super.admin@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .build();
    User savedUser = userRepository.saveAndFlush(user);

    String token = jwtTokenService.generateToken(savedUser);

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("super.admin@example.com"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.firstName").value("Super"))
        .andExpect(jsonPath("$.clientCode").doesNotExist())
        .andExpect(jsonPath("$.pickupLocation").doesNotExist())
        .andExpect(jsonPath("$.licensePlates").doesNotExist());
  }

  @Test
  @DisplayName("GET /api/v1/users/me without token returns 401 Unauthorized")
  void shouldDenyApiMeEndpointWhenTokenMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /v1/users/logout revokes token and denies subsequent requests")
  void shouldRevokeTokenOnLogoutAndDenySubsequentRequests() throws Exception {
    Client user =
        Client.builder()
            .firstName("Claire")
            .lastName("LogoutTest")
            .birthDate(LocalDate.of(1992, 4, 10))
            .email("claire.logout@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
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
  @DisplayName("GET /v1/users/{identifier} works with UUID and numeric ID for own account")
  void shouldAllowAccessByNumericIdAndUuidWithToken() throws Exception {
    Merchant user =
        Merchant.builder()
            .firstName("Sophie")
            .lastName("Polymorphic")
            .birthDate(LocalDate.of(1991, 7, 25))
            .email("sophie.poly@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
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
  @DisplayName("GET /v1/users/{userId} for another user without ADMIN role returns 403 Forbidden")
  void shouldDenyAccessToAnotherUserProfileForNonAdminUser() throws Exception {
    Client victim =
        Client.builder()
            .firstName("Victim")
            .lastName("User")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("victim@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .clientCode("CL11223")
            .build();
    User savedVictim = userRepository.saveAndFlush(victim);

    Client attacker =
        Client.builder()
            .firstName("Attacker")
            .lastName("User")
            .birthDate(LocalDate.of(1995, 2, 2))
            .email("attacker@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
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
  @DisplayName("GET /v1/users/{userId} for another user with ADMIN role returns 200 OK")
  void shouldAllowAdminToAccessAnyUserProfile() throws Exception {
    Client victim =
        Client.builder()
            .firstName("Victim2")
            .lastName("User2")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("victim2@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .clientCode("CL33445")
            .build();
    User savedVictim = userRepository.saveAndFlush(victim);

    Admin admin =
        Admin.builder()
            .firstName("Admin")
            .lastName("Boss")
            .birthDate(LocalDate.of(1980, 3, 3))
            .email("admin.security@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
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
  @DisplayName("POST /v1/users/stores records link between store and user")
  void shouldAllowStoreLinkEndpointWithToken() throws Exception {
    Merchant user =
        Merchant.builder()
            .firstName("Pierre")
            .lastName("Partner")
            .birthDate(LocalDate.of(1982, 11, 14))
            .email("pierre.partner@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
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
  @DisplayName("GET /v1/users/{userId} without token returns 401 Unauthorized")
  void shouldDenyGetUserByIdWithoutToken() throws Exception {
    mockMvc.perform(get("/v1/users/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /v1/auth/verify with shoploc_token cookie returns 200 and identity headers")
  void shouldAuthenticateViaSessionCookie() throws Exception {
    Client user =
        Client.builder()
            .firstName("Sophie")
            .lastName("Client")
            .birthDate(LocalDate.of(1995, 4, 20))
            .email("sophie.cookie@example.com")
            .passwordHash(passwordEncoder.encode("CookiePass1!"))
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

  @Test
  @DisplayName("GET /v1/admin/partners returns 401 without token")
  void shouldDenyAdminPartnersWithoutToken() throws Exception {
    mockMvc.perform(get("/v1/admin/partners")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /v1/admin/partners returns 403 with non-admin token")
  void shouldDenyAdminPartnersForNonAdmin() throws Exception {
    Client user =
        Client.builder()
            .firstName("Client")
            .lastName("User")
            .birthDate(LocalDate.of(1993, 3, 3))
            .email("client.partnercheck@example.com")
            .passwordHash(passwordEncoder.encode("Pass123!"))
            .clientCode("CL55667")
            .build();
    User saved = userRepository.saveAndFlush(user);
    String token = jwtTokenService.generateToken(saved);

    mockMvc
        .perform(get("/v1/admin/partners").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin partner operations succeed with admin token")
  void shouldAllowAdminToManagePartners() throws Exception {
    Admin admin =
        Admin.builder()
            .firstName("Super")
            .lastName("Admin")
            .birthDate(LocalDate.of(1985, 5, 5))
            .email("super.admin.partners@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .build();
    User savedAdmin = userRepository.saveAndFlush(admin);
    String adminToken = jwtTokenService.generateToken(savedAdmin);

    Merchant merchant =
        Merchant.builder()
            .firstName("Benoit")
            .lastName("Boulanger")
            .birthDate(LocalDate.of(1980, 8, 8))
            .email("benoit.partner@example.com")
            .passwordHash(passwordEncoder.encode("SecretPass1!"))
            .build();
    User savedMerchant = userRepository.saveAndFlush(merchant);

    mockMvc
        .perform(get("/v1/admin/partners").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    mockMvc
        .perform(
            get("/v1/admin/partners/" + savedMerchant.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("benoit.partner@example.com"));

    AdminAddStoreRequest addStoreRequest = new AdminAddStoreRequest(99L);
    mockMvc
        .perform(
            post("/v1/admin/partners/" + savedMerchant.getId() + "/stores")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addStoreRequest)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/v1/admin/partners/" + savedMerchant.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeIds[0]").value(99));
  }
}
