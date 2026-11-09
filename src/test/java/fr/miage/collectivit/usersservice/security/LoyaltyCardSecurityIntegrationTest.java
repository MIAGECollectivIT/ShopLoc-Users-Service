package fr.miage.collectivit.usersservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.usersservice.entity.Admin;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.entity.Merchant;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class LoyaltyCardSecurityIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenService jwtTokenService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  @DisplayName("GET /v1/users/loyalty-cards/{cardNumber} requires authentication (401)")
  void shouldRequireAuthenticationForLoyaltyCardScan() throws Exception {
    mockMvc.perform(get("/v1/users/loyalty-cards/k7P9xQ2")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /v1/users/loyalty-cards/{cardNumber} allows MERCHANT and returns masked data")
  void shouldAllowMerchantToIdentifyCustomerWithMaskedData() throws Exception {
    Client client =
        Client.builder()
            .firstName("Sophie")
            .lastName("Martin")
            .birthDate(LocalDate.of(1992, 4, 15))
            .email("sophie.martin@example.com")
            .passwordHash(passwordEncoder.encode("ClientPass1!"))
            .clientCode("CL98765")
            .pickupLocation("Mairie")
            .build();
    userRepository.saveAndFlush(client);

    Merchant merchant =
        Merchant.builder()
            .firstName("Paul")
            .lastName("Boucher")
            .birthDate(LocalDate.of(1985, 3, 20))
            .email("paul.boucher@example.com")
            .passwordHash(passwordEncoder.encode("MerchantPass1!"))
            .build();
    User savedMerchant = userRepository.saveAndFlush(merchant);
    String merchantToken = jwtTokenService.generateToken(savedMerchant);

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/CL98765")
                .header("Authorization", "Bearer " + merchantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientCode").value("CL98765"))
        .andExpect(jsonPath("$.firstName").value("Sophie"))
        .andExpect(jsonPath("$.lastNameMasked").value("M***n"))
        .andExpect(jsonPath("$.emailMasked").value("s***n@example.com"))
        .andExpect(jsonPath("$.recognized").value(true));
  }

  @Test
  @DisplayName("GET /v1/users/loyalty-cards/{cardNumber} allows ADMIN role")
  void shouldAllowAdminToIdentifyCustomer() throws Exception {
    Client client =
        Client.builder()
            .firstName("Luc")
            .lastName("Bernard")
            .birthDate(LocalDate.of(1988, 1, 10))
            .email("luc.bernard@example.com")
            .passwordHash(passwordEncoder.encode("ClientPass1!"))
            .clientCode("CL54321")
            .pickupLocation("Mairie")
            .build();
    userRepository.saveAndFlush(client);

    Admin admin =
        Admin.builder()
            .firstName("Admin")
            .lastName("Master")
            .birthDate(LocalDate.of(1980, 1, 1))
            .email("admin.root@example.com")
            .passwordHash(passwordEncoder.encode("AdminPass1!"))
            .build();
    User savedAdmin = userRepository.saveAndFlush(admin);
    String adminToken = jwtTokenService.generateToken(savedAdmin);

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/CL54321").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientCode").value("CL54321"))
        .andExpect(jsonPath("$.firstName").value("Luc"))
        .andExpect(jsonPath("$.recognized").value(true));
  }

  @Test
  @DisplayName("GET /v1/users/loyalty-cards/{cardNumber} rejects CLIENT role (403 Forbidden)")
  void shouldRejectClientRoleForLoyaltyCardScan() throws Exception {
    Client client =
        Client.builder()
            .firstName("Jean")
            .lastName("Valjean")
            .birthDate(LocalDate.of(1990, 1, 1))
            .email("jean.valjean@example.com")
            .passwordHash(passwordEncoder.encode("ClientPass1!"))
            .clientCode("CL11223")
            .pickupLocation("Mairie")
            .build();
    User savedClient = userRepository.saveAndFlush(client);
    String clientToken = jwtTokenService.generateToken(savedClient);

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/CL11223").header("Authorization", "Bearer " + clientToken))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.detail")
                .value("Access restricted to partner merchants and administrators"));
  }
}
