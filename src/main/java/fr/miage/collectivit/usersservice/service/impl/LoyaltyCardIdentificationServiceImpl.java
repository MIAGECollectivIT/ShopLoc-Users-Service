package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.dto.CustomerIdentificationResponse;
import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.exception.UserNotFoundException;
import fr.miage.collectivit.usersservice.repository.ClientRepository;
import fr.miage.collectivit.usersservice.service.LoyaltyCardIdentificationService;
import fr.miage.collectivit.usersservice.util.DataMaskingUtils;
import java.time.ZoneOffset;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default implementation of loyalty card identification service with data masking. */
@Service
@RequiredArgsConstructor
public class LoyaltyCardIdentificationServiceImpl implements LoyaltyCardIdentificationService {

  private static final Pattern CLIENT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{7}$");

  private final ClientRepository clientRepository;

  @Override
  @Transactional(readOnly = true)
  public CustomerIdentificationResponse identifyByCardNumber(String cardNumber) {
    if (cardNumber == null || !CLIENT_CODE_PATTERN.matcher(cardNumber.trim()).matches()) {
      throw new IllegalArgumentException(
          "Invalid client code format: expected exactly 7 alphanumeric characters");
    }

    String normalizedCode = cardNumber.trim();
    Client client =
        clientRepository
            .findByClientCode(normalizedCode)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "User not found with loyalty card: " + normalizedCode));

    Integer memberSinceYear =
        client.getCreatedAt() != null
            ? client.getCreatedAt().atZone(ZoneOffset.UTC).getYear()
            : null;

    return CustomerIdentificationResponse.builder()
        .publicId(client.getPublicId())
        .clientCode(client.getClientCode())
        .firstName(client.getFirstName())
        .lastNameMasked(DataMaskingUtils.maskLastName(client.getLastName()))
        .emailMasked(DataMaskingUtils.maskEmail(client.getEmail()))
        .recognized(true)
        .memberSinceYear(memberSinceYear)
        .build();
  }
}
