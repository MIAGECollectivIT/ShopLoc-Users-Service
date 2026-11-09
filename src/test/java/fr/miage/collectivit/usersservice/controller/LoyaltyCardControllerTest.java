package fr.miage.collectivit.usersservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.GlobalExceptionHandler;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.service.LoyaltyCardIdentificationService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
  void shouldIdentifyUserByCardNumber() throws Exception {
    String cardNumber = "aB3dE7K";
    UserResponse response =
        UserResponse.builder()
            .id(UUID.randomUUID())
            .email("card.holder@test.com")
            .role(UserRole.CLIENT)
            .firstName("Luc")
            .lastName("Dupont")
            .birthDate(LocalDate.of(1990, 1, 1))
            .clientCode(cardNumber)
            .pickupLocation("Mairie")
            .build();

    when(loyaltyCardIdentificationService.identifyByCardNumber(cardNumber)).thenReturn(response);

    mockMvc
        .perform(get("/v1/users/loyalty-cards/{cardNumber}", cardNumber))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientCode").value(cardNumber))
        .andExpect(jsonPath("$.email").value("card.holder@test.com"));
  }

  @Test
  void shouldReturnNotFoundWhenCardNumberDoesNotExist() throws Exception {
    String unknownCard = "UNKNOWN";
    when(loyaltyCardIdentificationService.identifyByCardNumber(unknownCard))
        .thenThrow(new UserNotFoundException("User not found with loyalty card: " + unknownCard));

    mockMvc
        .perform(get("/v1/users/loyalty-cards/{cardNumber}", unknownCard))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("User not found with loyalty card: " + unknownCard));
  }
}
