package fr.miage.collectivit.usersservice.service.impl;

import fr.miage.collectivit.usersservice.entity.CardNotificationStatus;
import fr.miage.collectivit.usersservice.entity.PhysicalCardNotification;
import fr.miage.collectivit.usersservice.entity.User;
import fr.miage.collectivit.usersservice.repository.PhysicalCardNotificationRepository;
import fr.miage.collectivit.usersservice.service.PhysicalCardNotificationService;
import fr.miage.collectivit.usersservice.service.mail.PhysicalCardEmailService;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.AvoidCatchingGenericException"})
public class PhysicalCardNotificationServiceImpl implements PhysicalCardNotificationService {

  private final PhysicalCardNotificationRepository notificationRepository;
  private final PhysicalCardEmailService physicalCardEmailService;

  public PhysicalCardNotificationServiceImpl(
      PhysicalCardNotificationRepository notificationRepository,
      PhysicalCardEmailService physicalCardEmailService) {
    this.notificationRepository = notificationRepository;
    this.physicalCardEmailService = physicalCardEmailService;
  }

  @Override
  @Transactional
  public PhysicalCardNotification processRegistrationCardRequest(User user, String pickupLocation) {
    if (user == null) {
      log.warn("Cannot process physical card request: user is null");
      return null;
    }

    String effectivePickup =
        (pickupLocation != null && !pickupLocation.isBlank())
            ? pickupLocation.trim()
            : user.getPickupLocation();

    if (effectivePickup == null || effectivePickup.isBlank()) {
      log.debug("No physical card requested for user ID {}", user.getId());
      return null;
    }

    String clientCode = user.getClientCode();
    if (clientCode == null || clientCode.isBlank()) {
      log.warn("Cannot process physical card request: user ID {} has no clientCode", user.getId());
      return null;
    }

    String recipientEmail = physicalCardEmailService.getDefaultRecipientEmail();

    PhysicalCardNotification notification =
        PhysicalCardNotification.builder()
            .userId(user.getId())
            .clientCode(clientCode)
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .userEmail(user.getEmail())
            .pickupLocation(effectivePickup)
            .serviceRecipientEmail(recipientEmail)
            .status(CardNotificationStatus.PENDING)
            .build();

    notification = notificationRepository.save(notification);

    try {
      physicalCardEmailService.sendPhysicalCardRequestEmail(
          recipientEmail,
          user.getFirstName(),
          user.getLastName(),
          user.getEmail(),
          clientCode,
          effectivePickup);

      notification.setStatus(CardNotificationStatus.SENT);
      notification.setSentAt(Instant.now());
      notification.setErrorMessage(null);
      log.info(
          "Physical card notification successfully logged and sent for user {} ({})",
          user.getId(),
          clientCode);
    } catch (Exception ex) {
      notification.setStatus(CardNotificationStatus.FAILED);
      notification.setErrorMessage(ex.getMessage());
      log.error(
          "Failed to dispatch physical card request email for user {} ({}): {}",
          user.getId(),
          clientCode,
          ex.getMessage());
    }

    return notificationRepository.save(notification);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PhysicalCardNotification> getNotificationsForUser(Long userId) {
    if (userId == null) {
      return List.of();
    }
    return notificationRepository.findByUserId(userId);
  }
}
