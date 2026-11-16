package fr.miage.collectivit.usersservice.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fr.miage.collectivit.usersservice.entity.Client;
import fr.miage.collectivit.usersservice.service.PhysicalCardNotificationService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhysicalCardNotificationListenerTest {

  @Mock private PhysicalCardNotificationService notificationService;

  private PhysicalCardNotificationListener listener;

  @BeforeEach
  void setUp() {
    listener = new PhysicalCardNotificationListener(notificationService);
  }

  @Test
  @DisplayName("onUserRegistered should delegate to notificationService when valid event received")
  void shouldDelegateToNotificationService() {
    Client client =
        Client.builder()
            .id(10L)
            .firstName("Bob")
            .lastName("Dylan")
            .birthDate(LocalDate.of(1950, 5, 24))
            .email("bob.dylan@test.fr")
            .clientCode("C111222")
            .pickupLocation("Mairie Centrale")
            .build();

    UserRegisteredEvent event = new UserRegisteredEvent(client, "Mairie Centrale");

    listener.onUserRegistered(event);

    verify(notificationService).processRegistrationCardRequest(client, "Mairie Centrale");
  }

  @Test
  @DisplayName("onUserRegistered should safely ignore null event or null user")
  void shouldSafelyIgnoreNullEvent() {
    listener.onUserRegistered(null);
    listener.onUserRegistered(new UserRegisteredEvent(null, null));

    verifyNoInteractions(notificationService);
  }
}
