package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.UserResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.ClientRepository;
import fr.miage.collectivit.usersservice.service.LoyaltyCardIdentificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoyaltyCardIdentificationServiceImpl implements LoyaltyCardIdentificationService {

  private final ClientRepository clientRepository;

  @Override
  @Transactional(readOnly = true)
  public UserResponse identifyByCardNumber(String cardNumber) {
    Client client =
        clientRepository
            .findByClientCode(cardNumber)
            .orElseThrow(
                () -> new UserNotFoundException("User not found with loyalty card: " + cardNumber));
    return UserResponse.fromEntity(client);
  }
}
