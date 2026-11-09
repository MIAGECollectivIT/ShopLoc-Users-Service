package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.CustomerIdentificationResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.ClientRepository;
import fr.miage.collectivit.usersservice.service.impl.LoyaltyCardIdentificationServiceImpl;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoyaltyCardIdentificationServiceTest {

  @Mock private ClientRepository clientRepository;

  @InjectMocks private LoyaltyCardIdentificationServiceImpl loyaltyCardIdentificationService;

  @Test
  @DisplayName("Should identify customer and return masked sensitive data")
  void shouldIdentifyUserByCardNumberWithMaskedData() {
    String cardNumber = "k7P9xQ2";
    Client user =
        Client.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .email("client.dupont@loyalty.com")
            .passwordHash("$2a$12$fakeHash")
            .firstName("Luc")
            .lastName("Garnier")
            .birthDate(LocalDate.of(1982, 11, 3))
            .clientCode(cardNumber)
            .pickupLocation("Mairie")
            .createdAt(Instant.parse("2026-05-10T14:30:00Z"))
            .build();

    when(clientRepository.findByClientCode(cardNumber)).thenReturn(Optional.of(user));

    CustomerIdentificationResponse response =
        loyaltyCardIdentificationService.identifyByCardNumber(cardNumber);

    assertThat(response.clientCode()).isEqualTo(cardNumber);
    assertThat(response.firstName()).isEqualTo("Luc");
    assertThat(response.lastNameMasked()).isEqualTo("G***r");
    assertThat(response.emailMasked()).isEqualTo("c***t@loyalty.com");
    assertThat(response.recognized()).isTrue();
    assertThat(response.memberSinceYear()).isEqualTo(2026);
  }

  @Test
  @DisplayName("Should throw UserNotFoundException when client code is not found in database")
  void shouldThrowWhenCardNumberNotFound() {
    String unknownCard = "UNKNOWN";
    when(clientRepository.findByClientCode(unknownCard)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loyaltyCardIdentificationService.identifyByCardNumber(unknownCard))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("User not found with loyalty card: UNKNOWN");
  }

  @ParameterizedTest
  @ValueSource(strings = {"12345", "12345678", "ab@1234", "       ", ""})
  @DisplayName("Should reject invalid client code format")
  void shouldRejectInvalidClientCode(String invalidCode) {
    assertThatThrownBy(() -> loyaltyCardIdentificationService.identifyByCardNumber(invalidCode))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid client code format");
  }
}
