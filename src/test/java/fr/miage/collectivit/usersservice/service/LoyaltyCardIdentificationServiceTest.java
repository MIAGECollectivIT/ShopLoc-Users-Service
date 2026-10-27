package fr.miage.collectivit.usersservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.entity.UserRole;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.UserRepository;
import fr.miage.collectivit.usersservice.service.impl.LoyaltyCardIdentificationServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoyaltyCardIdentificationServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private LoyaltyCardIdentificationServiceImpl loyaltyCardIdentificationService;

  @Test
  void shouldIdentifyUserByCardNumber() {
    String cardNumber = "k7P9xQ2";
    User user =
        User.builder()
            .id(1L)
            .publicId(UUID.randomUUID())
            .email("client@loyalty.com")
            .passwordHash("$2a$12$fakeHash")
            .role(UserRole.CLIENT)
            .firstName("Luc")
            .lastName("Garnier")
            .birthDate(LocalDate.of(1982, 11, 3))
            .clientCode(cardNumber)
            .pickupLocation("Mairie")
            .build();

    when(userRepository.findByClientCode(cardNumber)).thenReturn(Optional.of(user));

    UserResponse response = loyaltyCardIdentificationService.identifyByCardNumber(cardNumber);

    assertThat(response.clientCode()).isEqualTo(cardNumber);
    assertThat(response.firstName()).isEqualTo("Luc");
    assertThat(response.lastName()).isEqualTo("Garnier");
  }

  @Test
  void shouldThrowWhenCardNumberNotFound() {
    String unknownCard = "UNKNOWN";
    when(userRepository.findByClientCode(unknownCard)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loyaltyCardIdentificationService.identifyByCardNumber(unknownCard))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(unknownCard);
  }
}
