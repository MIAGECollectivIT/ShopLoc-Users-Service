package fr.miage.collectivit.usersservice.event;

import fr.miage.collectivit.usersservice.service.PhysicalCardNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.AvoidCatchingGenericException"})
public class PhysicalCardNotificationListener {

  private final PhysicalCardNotificationService notificationService;

  public PhysicalCardNotificationListener(PhysicalCardNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onUserRegistered(UserRegisteredEvent event) {
    if (event == null || event.user() == null) {
      return;
    }
    log.info(
        "Handling post-registration physical card notification event for user ID {}",
        event.user().getId());
    try {
      notificationService.processRegistrationCardRequest(event.user(), event.pickupLocation());
    } catch (Exception ex) {
      log.error(
          "Unexpected error in physical card notification listener for user ID {}: {}",
          event.user().getId(),
          ex.getMessage(),
          ex);
    }
  }
}
