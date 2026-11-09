package fr.miage.collectivit.usersservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.usersservice.dto.CustomerIdentificationResponse;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.usersservice.service.LoyaltyCardIdentificationService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LoyaltyCardControllerTest {

  @Mock private LoyaltyCardIdentificationService loyaltyCardIdentificationService;

  @InjectMocks private LoyaltyCardController loyaltyCardController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(loyaltyCardController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("Should successfully identify user when caller is MERCHANT")
  void shouldIdentifyUserByCardNumberWithMerchantRole() throws Exception {
    String cardNumber = "aB3dE7K";
    CustomerIdentificationResponse response =
        CustomerIdentificationResponse.builder()
            .publicId(UUID.randomUUID())
            .clientCode(cardNumber)
            .firstName("Luc")
            .lastNameMasked("D***t")
            .emailMasked("c***t@test.com")
            .recognized(true)
            .memberSinceYear(2026)
            .build();

    when(loyaltyCardIdentificationService.identifyByCardNumber(cardNumber)).thenReturn(response);

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/{cardNumber}", cardNumber)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientCode").value(cardNumber))
        .andExpect(jsonPath("$.firstName").value("Luc"))
        .andExpect(jsonPath("$.lastNameMasked").value("D***t"))
        .andExpect(jsonPath("$.emailMasked").value("c***t@test.com"))
        .andExpect(jsonPath("$.recognized").value(true))
        .andExpect(jsonPath("$.memberSinceYear").value(2026));
  }

  @Test
  @DisplayName("Should successfully identify user when caller is ADMIN")
  void shouldIdentifyUserByCardNumberWithAdminRole() throws Exception {
    String cardNumber = "k7P9xQ2";
    CustomerIdentificationResponse response =
        CustomerIdentificationResponse.builder()
            .publicId(UUID.randomUUID())
            .clientCode(cardNumber)
            .firstName("Alice")
            .lastNameMasked("M***n")
            .emailMasked("a***e@test.com")
            .recognized(true)
            .memberSinceYear(2025)
            .build();

    when(loyaltyCardIdentificationService.identifyByCardNumber(cardNumber)).thenReturn(response);

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/{cardNumber}", cardNumber)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientCode").value(cardNumber))
        .andExpect(jsonPath("$.firstName").value("Alice"))
        .andExpect(jsonPath("$.recognized").value(true));
  }

  @Test
  @DisplayName("Should return 403 Forbidden when caller has CLIENT role")
  void shouldRejectAccessForClientRole() throws Exception {
    String cardNumber = "aB3dE7K";

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/{cardNumber}", cardNumber)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "CLIENT"))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.detail")
                .value("Access restricted to partner merchants and administrators"));
  }

  @Test
  @DisplayName("Should return 403 Forbidden when no role attribute is present")
  void shouldRejectAccessWhenNoRoleProvided() throws Exception {
    String cardNumber = "aB3dE7K";

    mockMvc
        .perform(get("/v1/users/loyalty-cards/{cardNumber}", cardNumber))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.detail")
                .value("Access restricted to partner merchants and administrators"));
  }

  @Test
  @DisplayName("Should return 400 Bad Request when card number format is invalid")
  void shouldReturnBadRequestWhenCardFormatInvalid() throws Exception {
    String invalidCode = "INVALID_LENGTH";

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/{cardNumber}", invalidCode)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Client card code must be exactly 7 alphanumeric characters"));
  }

  @Test
  @DisplayName("Should return 404 Not Found when card number does not exist")
  void shouldReturnNotFoundWhenCardNumberDoesNotExist() throws Exception {
    String unknownCard = "UNKNOWN";
    when(loyaltyCardIdentificationService.identifyByCardNumber(unknownCard))
        .thenThrow(new UserNotFoundException("User not found with loyalty card: " + unknownCard));

    mockMvc
        .perform(
            get("/v1/users/loyalty-cards/{cardNumber}", unknownCard)
                .requestAttr(JwtAuthInterceptor.ATTR_USER_ROLE, "MERCHANT"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("User not found with loyalty card: " + unknownCard));
  }
}
